/**
 * Order payment helpers — advance vs balance amounts and status
 */

function normalizeOrderStatus(order) {
  if (!order) return "pending";
  if (order.status === "paid") return "advance_paid";
  return order.status || "pending";
}

function getAdvancePaidInr(order) {
  if (!order) return 0;
  if (Number(order.advance_paid_inr) > 0) return Number(order.advance_paid_inr);
  const st = normalizeOrderStatus(order);
  if (st === "advance_paid" || st === "completed") return Number(order.amount_inr) || 0;
  return 0;
}

function getBalancePaidInr(order) {
  return Number(order?.balance_paid_inr) || 0;
}

function getBalanceDue(order) {
  if (!order) return 0;
  const total = Number(order.total_inr) || 0;
  const paid = getAdvancePaidInr(order) + getBalancePaidInr(order);
  return Math.max(0, total - paid);
}

function isFullyPaid(order) {
  return normalizeOrderStatus(order) === "completed" || getBalanceDue(order) === 0;
}

function canPayBalance(order) {
  if (!order) return false;
  if (normalizeOrderStatus(order) !== "advance_paid") return false;
  if (!order.delivered_at) return false;
  return getBalanceDue(order) > 0;
}

function orderStatusLabel(order) {
  const st = normalizeOrderStatus(order);
  const due = getBalanceDue(order);
  const total = Number(order?.total_inr) || 0;
  if (st === "completed" || (st === "advance_paid" && due <= 0 && total > 0)) return "Full paid";
  if (st === "advance_paid" && order.delivered_at && due > 0) {
    return "Balance due";
  }
  if (st === "advance_paid") return "Advance paid";
  if (st === "pending") return "Advance pending";
  return st;
}

module.exports = {
  normalizeOrderStatus,
  getAdvancePaidInr,
  getBalancePaidInr,
  getBalanceDue,
  isFullyPaid,
  canPayBalance,
  orderStatusLabel,
};
