/**
 * =============================================================================
 * BACKEND — payments controller
 * File: src/controllers/payments.controller.js
 * Order pages, Razorpay API, webhook (advance + balance)
 * =============================================================================
 */

const { brand, navItems, company } = require("../config/site");
const {
  isPaymentEnabled,
  getRazorpayKeyId,
  getAdvancePercent,
  isLiveKeyOnLocalhost,
} = require("../config/payments");
const paymentsService = require("../services/payments.service");
const { orderStatusLabel } = require("../services/order-payment.helpers");

function renderWithLayout(res, view, page, extra = {}) {
  res.render(view, {
    page,
    navItems,
    brand,
    year: new Date().getFullYear(),
    authUser: res.locals.authUser || null,
    isAdmin: !!res.locals.isAdmin,
    googleLoginEnabled: !!res.locals.googleLoginEnabled,
    paymentEnabled: isPaymentEnabled(),
    paymentAdvancePercent: getAdvancePercent(),
    razorpayKeyId: getRazorpayKeyId(),
    paymentLiveOnLocalhost: isLiveKeyOnLocalhost(),
    ...extra,
  });
}

function showOrder(req, res) {
  const q = req.query || {};
  const totalInr = Math.floor(Number(q.total) || 0);
  const amountInr =
    totalInr > 0
      ? paymentsService.computeAdvanceInr(totalInr)
      : Math.floor(Number(q.amount) || 0);

  renderWithLayout(res, "pages/order", { key: "order", title: "Place Order & Pay | Shrisha Technology" }, {
    orderPrefill: {
      service: typeof q.service === "string" ? q.service : "",
      plan: typeof q.plan === "string" ? q.plan : "",
      total_inr: totalInr,
      amount_inr: amountInr,
    },
  });
}

async function showOrderSuccess(req, res) {
  const id = typeof req.query.id === "string" ? req.query.id : "";
  const order = id ? await paymentsService.getOrderByPublicId(id) : null;
  const st = order ? paymentsService.normalizeOrderStatus(order) : "";
  const receipt =
    order && (st === "advance_paid" || st === "completed")
      ? paymentsService.buildReceiptData(order, "advance")
      : null;

  renderWithLayout(
    res,
    "pages/order-success",
    { key: "order-success", title: "Order Confirmed | Shrisha Technology" },
    {
      order,
      receipt,
      balanceDue: order ? paymentsService.getBalanceDue(order) : 0,
      orderStatusLabel: order ? orderStatusLabel(order) : "",
    },
  );
}

async function showPayAdvance(req, res) {
  const id = typeof req.query.id === "string" ? req.query.id : "";
  const order = id ? await paymentsService.getOrderByPublicId(id) : null;
  const st = order ? paymentsService.normalizeOrderStatus(order) : "";
  const canPay = st === "pending";
  const alreadyPaid = st === "advance_paid" || st === "completed";

  renderWithLayout(
    res,
    "pages/order-pay-advance",
    { key: "order-pay-advance", title: "Pay Advance | Shrisha Technology" },
    {
      order,
      canPay,
      alreadyPaid,
      advanceDue: order ? Number(order.amount_inr) || 0 : 0,
      orderStatusLabel: order ? orderStatusLabel(order) : "",
    },
  );
}

async function showPayBalance(req, res) {
  const id = typeof req.query.id === "string" ? req.query.id : "";
  const order = id ? await paymentsService.getOrderByPublicId(id) : null;
  const st = order ? paymentsService.normalizeOrderStatus(order) : "";
  const balanceDue = order ? paymentsService.getBalanceDue(order) : 0;
  const canPay = order ? paymentsService.canPayBalance(order) : false;
  const alreadyComplete = st === "completed";

  renderWithLayout(
    res,
    "pages/order-pay-balance",
    { key: "order-pay-balance", title: "Pay Balance | Shrisha Technology" },
    {
      order,
      balanceDue,
      canPay,
      alreadyComplete,
      advancePaid: order ? paymentsService.getAdvancePaidInr(order) : 0,
      orderStatusLabel: order ? orderStatusLabel(order) : "",
    },
  );
}

async function showBalanceSuccess(req, res) {
  const id = typeof req.query.id === "string" ? req.query.id : "";
  const order = id ? await paymentsService.getOrderByPublicId(id) : null;
  const st = order ? paymentsService.normalizeOrderStatus(order) : "";
  const receipt = order && st === "completed" ? paymentsService.buildReceiptData(order, "balance") : null;

  renderWithLayout(
    res,
    "pages/order-balance-success",
    { key: "order-balance-success", title: "Payment Complete | Shrisha Technology" },
    { order, receipt },
  );
}

async function showOrderReceipt(req, res) {
  const id = typeof req.query.id === "string" ? req.query.id : "";
  const phase = req.query.phase === "balance" ? "balance" : "advance";
  const order = id ? await paymentsService.getOrderByPublicId(id) : null;

  if (!order) {
    return res.status(404).send("Receipt not found. Complete payment first or check your order ID.");
  }

  const st = paymentsService.normalizeOrderStatus(order);
  if (phase === "balance") {
    if (st !== "completed") {
      return res.status(404).send("Balance receipt not available yet.");
    }
  } else if (st === "pending") {
    return res.status(404).send("Receipt not found. Complete payment first or check your order ID.");
  }

  const receipt = paymentsService.buildReceiptData(order, phase);
  const baseUrl = process.env.BASE_URL || `${req.protocol}://${req.get("host")}`;

  res.render("pages/order-receipt", {
    page: { title: `Receipt ${order.public_id} | ${brand}` },
    company,
    receipt,
    order,
    brand,
    year: new Date().getFullYear(),
    baseUrl,
  });
}

async function createOrder(req, res) {
  try {
    const { name, email, phone, service, plan, notes, total_inr } = req.body || {};

    if (!name?.trim() || !email?.trim()) {
      return res.status(400).json({ ok: false, error: "Name and email are required." });
    }
    if (!service?.trim() || !plan?.trim()) {
      return res.status(400).json({ ok: false, error: "Service and plan are required." });
    }

    const result = await paymentsService.createPaymentOrder({
      name,
      email,
      phone,
      service,
      plan,
      notes,
      total_inr,
    });

    if (!result.paymentConfigured) {
      return res.status(503).json({
        ok: false,
        error: result.message,
        publicId: result.order.public_id,
        amountInr: result.order.amount_inr,
      });
    }

    return res.json({
      ok: true,
      publicId: result.order.public_id,
      razorpayOrderId: result.razorpayOrderId,
      amountPaise: result.amountPaise,
      amountInr: result.order.amount_inr,
      keyId: result.keyId,
      customer: {
        name: result.order.name,
        email: result.order.email,
        contact: result.order.phone || "",
      },
      description: `${result.order.service} — ${result.order.plan} (advance)`,
    });
  } catch (err) {
    console.error("[payments] create-order", err);
    return res.status(500).json({ ok: false, error: err.message || "Could not create order." });
  }
}

async function createBalanceOrder(req, res) {
  try {
    const publicId =
      typeof req.body?.publicId === "string"
        ? req.body.publicId.trim()
        : typeof req.query?.id === "string"
          ? req.query.id.trim()
          : "";

    if (!publicId) {
      return res.status(400).json({ ok: false, error: "Order ID is required." });
    }

    const result = await paymentsService.createBalancePaymentOrder(publicId);

    if (!result.paymentConfigured) {
      return res.status(503).json({
        ok: false,
        error: result.message,
        publicId: result.order.public_id,
        amountInr: paymentsService.getBalanceDue(result.order),
      });
    }

    return res.json({
      ok: true,
      publicId: result.order.public_id,
      razorpayOrderId: result.razorpayOrderId,
      amountPaise: result.amountPaise,
      amountInr: result.amountInr,
      keyId: result.keyId,
      customer: {
        name: result.order.name,
        email: result.order.email,
        contact: result.order.phone || "",
      },
      description: `${result.order.service} — ${result.order.plan} (balance)`,
    });
  } catch (err) {
    console.error("[payments] create-balance-order", err);
    return res.status(400).json({ ok: false, error: err.message || "Could not create balance payment." });
  }
}

async function resumeAdvanceOrder(req, res) {
  try {
    const publicId =
      typeof req.body?.publicId === "string"
        ? req.body.publicId.trim()
        : typeof req.query?.id === "string"
          ? req.query.id.trim()
          : "";

    if (!publicId) {
      return res.status(400).json({ ok: false, error: "Order ID is required." });
    }

    const result = await paymentsService.resumeAdvancePaymentOrder(publicId);

    if (!result.paymentConfigured) {
      return res.status(503).json({
        ok: false,
        error: result.message,
        publicId: result.order.public_id,
        amountInr: result.order.amount_inr,
      });
    }

    return res.json({
      ok: true,
      publicId: result.order.public_id,
      razorpayOrderId: result.razorpayOrderId,
      amountPaise: result.amountPaise,
      amountInr: result.amountInr,
      keyId: result.keyId,
      customer: {
        name: result.order.name,
        email: result.order.email,
        contact: result.order.phone || "",
      },
      description: `${result.order.service} — ${result.order.plan} (advance)`,
    });
  } catch (err) {
    console.error("[payments] resume-advance", err);
    return res.status(400).json({ ok: false, error: err.message || "Could not resume advance payment." });
  }
}

async function verifyPayment(req, res) {
  try {
    const { razorpay_order_id, razorpay_payment_id, razorpay_signature } = req.body || {};
    if (!razorpay_order_id || !razorpay_payment_id) {
      return res.status(400).json({ ok: false, error: "Missing payment details." });
    }

    const { order, alreadyPaid } = await paymentsService.completePayment({
      razorpay_order_id,
      razorpay_payment_id,
      razorpay_signature,
    });

    return res.json({
      ok: true,
      alreadyPaid,
      publicId: order.public_id,
      redirectUrl: `/order/success?id=${encodeURIComponent(order.public_id)}`,
    });
  } catch (err) {
    console.error("[payments] verify", err);
    return res.status(400).json({ ok: false, error: err.message || "Payment verification failed." });
  }
}

async function verifyBalancePayment(req, res) {
  try {
    const { razorpay_order_id, razorpay_payment_id, razorpay_signature } = req.body || {};
    if (!razorpay_order_id || !razorpay_payment_id) {
      return res.status(400).json({ ok: false, error: "Missing payment details." });
    }

    const { order, alreadyPaid } = await paymentsService.completeBalancePayment({
      razorpay_order_id,
      razorpay_payment_id,
      razorpay_signature,
    });

    return res.json({
      ok: true,
      alreadyPaid,
      publicId: order.public_id,
      redirectUrl: `/order/balance-success?id=${encodeURIComponent(order.public_id)}`,
    });
  } catch (err) {
    console.error("[payments] verify-balance", err);
    return res.status(400).json({ ok: false, error: err.message || "Payment verification failed." });
  }
}

async function paymentStatus(req, res) {
  try {
    const publicId =
      typeof req.query.publicId === "string"
        ? req.query.publicId
        : typeof req.body?.publicId === "string"
          ? req.body.publicId
          : "";
    if (!publicId) {
      return res.status(400).json({ ok: false, error: "Order reference required." });
    }

    const result = await paymentsService.checkOrderPaymentStatus(publicId);
    return res.json({ ok: true, ...result });
  } catch (err) {
    console.error("[payments] status", err);
    return res.status(500).json({ ok: false, error: err.message || "Could not check payment." });
  }
}

async function balancePaymentStatus(req, res) {
  try {
    const publicId =
      typeof req.query.publicId === "string"
        ? req.query.publicId
        : typeof req.body?.publicId === "string"
          ? req.body.publicId
          : "";
    if (!publicId) {
      return res.status(400).json({ ok: false, error: "Order reference required." });
    }

    const result = await paymentsService.checkBalancePaymentStatus(publicId);
    return res.json({ ok: true, ...result });
  } catch (err) {
    console.error("[payments] balance-status", err);
    return res.status(500).json({ ok: false, error: err.message || "Could not check payment." });
  }
}

async function razorpayWebhook(req, res) {
  try {
    const signature = req.get("x-razorpay-signature") || "";
    const rawBody = req.body;

    if (!Buffer.isBuffer(rawBody)) {
      return res.status(400).json({ ok: false, error: "Invalid webhook body." });
    }

    if (!paymentsService.verifyWebhookSignature(rawBody, signature)) {
      return res.status(401).json({ ok: false, error: "Invalid webhook signature." });
    }

    const payload = JSON.parse(rawBody.toString("utf8"));
    await paymentsService.handleWebhookEvent(payload);
    return res.json({ ok: true });
  } catch (err) {
    console.error("[payments] webhook", err);
    return res.status(500).json({ ok: false });
  }
}

module.exports = {
  showOrder,
  showOrderSuccess,
  showPayAdvance,
  showPayBalance,
  showBalanceSuccess,
  showOrderReceipt,
  createOrder,
  createBalanceOrder,
  resumeAdvanceOrder,
  verifyPayment,
  verifyBalancePayment,
  paymentStatus,
  balancePaymentStatus,
  razorpayWebhook,
};
