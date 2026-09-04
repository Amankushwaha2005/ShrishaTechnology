/**
 * =============================================================================
 * CLIENT — order-balance-page.js
 * URL: /order/pay-balance
 * File: order-balance-page.js
 * Razorpay checkout for remaining balance
 * =============================================================================
 */

(function () {
  const panel = document.getElementById("balance-payment-panel");
  if (!panel) return;

  const paymentEnabled = panel.dataset.paymentEnabled === "1";
  const publicId = panel.dataset.publicId || "";
  const errEl = document.getElementById("balance-form-error");
  const payBtn = document.getElementById("balance-pay-razorpay");
  let pollTimer = null;

  const liveOnLocal =
    panel.dataset.liveOnLocal === "1" ||
    /localhost|127\.0\.0\.1/.test(window.location.hostname);

  function showError(msg) {
    if (!errEl) return;
    errEl.textContent = msg;
    errEl.hidden = !msg;
  }

  function stopPoll() {
    if (pollTimer) {
      clearInterval(pollTimer);
      pollTimer = null;
    }
  }

  function startPoll() {
    stopPoll();
    pollTimer = setInterval(async () => {
      try {
        const res = await fetch(
          `/api/payments/balance-status?publicId=${encodeURIComponent(publicId)}`,
        );
        const data = await res.json();
        if (data.ok && data.status === "paid" && data.redirectUrl) {
          stopPoll();
          window.location.href = data.redirectUrl;
        }
      } catch (_) {
        /* ignore */
      }
    }, 2500);
  }

  async function verifyPayment(response) {
    const res = await fetch("/api/payments/verify-balance", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        razorpay_order_id: response.razorpay_order_id,
        razorpay_payment_id: response.razorpay_payment_id,
        razorpay_signature: response.razorpay_signature,
      }),
    });
    const data = await res.json();
    if (!data.ok) throw new Error(data.error || "Verification failed.");
    window.location.href = data.redirectUrl;
  }

  async function openRazorpay() {
    if (!paymentEnabled) {
      showError("Online payment is not available. Contact us on WhatsApp.");
      return;
    }
    if (liveOnLocal) {
      showError(
        "Live Razorpay keys may not work on localhost. Use test keys locally or pay on the live site.",
      );
    }

    showError("");
    if (payBtn) payBtn.disabled = true;

    try {
      const res = await fetch("/api/payments/create-balance-order", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ publicId }),
      });
      const data = await res.json();
      if (!data.ok) throw new Error(data.error || "Could not start payment.");

      if (typeof Razorpay === "undefined") {
        throw new Error("Payment library failed to load. Refresh and try again.");
      }

      const options = {
        key: data.keyId,
        amount: data.amountPaise,
        currency: "INR",
        name: "Shrisha Technology",
        description: data.description,
        order_id: data.razorpayOrderId,
        prefill: {
          name: panel.dataset.customerName || "",
          email: panel.dataset.customerEmail || "",
          contact: panel.dataset.customerPhone || "",
        },
        theme: { color: "#c9a227" },
        handler: async (response) => {
          try {
            await verifyPayment(response);
          } catch (err) {
            showError(err.message || "Payment verification failed.");
            startPoll();
          }
        },
        modal: {
          ondismiss: () => {
            if (payBtn) payBtn.disabled = false;
            startPoll();
          },
        },
      };

      const rzp = new Razorpay(options);
      rzp.on("payment.failed", (resp) => {
        showError(resp.error?.description || "Payment failed. Try again.");
        if (payBtn) payBtn.disabled = false;
      });
      rzp.open();
      startPoll();
    } catch (err) {
      showError(err.message || "Could not open checkout.");
      if (payBtn) payBtn.disabled = false;
    }
  }

  if (payBtn) {
    payBtn.addEventListener("click", openRazorpay);
  }

  window.addEventListener("beforeunload", stopPoll);
})();
