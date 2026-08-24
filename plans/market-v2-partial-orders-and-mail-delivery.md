# Market UI / Transaction v2 Plan

## Goals

Upgrade the player market from a whole-order card list into a live, sortable order browser with inline details, partial fills, and mailbox-based demand settlement.

## UX structure

- Left ~70-72%: order browser.
- Right ~28-30%: selected-order details/actions.
- Top controls: search, filters, sort selector, create sales, create demand.
- Bottom left: pagination only.
- Selecting an order adds a restrained glow border only; no background replacement or side stripe.
- Clicking browser blank space clears the detail pane.

## Sorting

Add a stable server-side `MarketOrderSort` enum. First version:

1. DEFAULT
2. UNIT_PRICE_ASC
3. UNIT_PRICE_DESC
4. NEWEST
5. EXPIRING_SOON

Sorting must happen on the server before pagination:

`filter -> search -> sort -> page slice`

Never sort only the current client page.

For equal sort keys, add deterministic tie-breakers (listingTime / tradeId) so pages do not flicker between refreshes.

## Pricing model

Partial fills require an unambiguous integer unit price.

### New orders

- Creation UI accepts `unitPrice`, not an editable total price.
- `totalPrice = unitPrice * quantity` is calculated server-side with overflow checks.
- UI displays total price as read-only derived data.

### Legacy orders

Do not rewrite persisted value.

- If `totalPrice % quantity == 0`, the legacy order has an exact integer unit price and may support partial fills.
- If not divisible, expose it as `whole-order only` until it is completed/cancelled/expired.
- Sorting may compare legacy non-divisible unit prices as rational values (`totalPrice / quantity`) without mutating persisted data.

## Partial SALES purchases

Example: listing 64 diamonds at unit price 20; buyer purchases 10.

- Requested quantity must satisfy `1 <= q <= remainingQuantity`.
- Charged amount is `unitPrice * q`.
- Buyer inventory capacity check is only for q.
- Buyer balance check is only for charged amount.
- Seller balance-limit check is only for charged amount.
- Market repository performs an atomic compare-and-swap quantity transition against the expected order snapshot.
- Partial fill updates the same tradeId:
  - `quantity = remainingQuantity - q`
  - `totalPrice = remainingTotalPrice - chargedAmount`
- If q consumes the full remaining quantity, remove the order.
- Rollback must restore the exact previous order snapshot if inventory/payment later fails.
- Never implement partial purchase as “remove full order then create a new order”; that is race-prone and can oversell.

Right detail panel for SALES:

- item icon/name
- remaining quantity
- unit price
- selected purchase quantity
- total charge
- seller
- expiration
- buyer balance
- buyer inventory capacity
- purchase button

Client preflight disables purchase for insufficient balance/capacity, but the server remains authoritative.

## Partial DEMAND fulfillment

Make demand symmetric with sales.

Example: requester wants 64 diamonds at unit price 20; supplier owns 10 and fulfills 10.

- Requester funds are still frozen/debited at order creation.
- Supplier chooses `1 <= q <= min(remainingDemand, supplierMatchingItems)`.
- Supplier payout is `unitPrice * q`.
- Remaining order quantity and remaining frozen value are atomically reduced.
- Final partial fill removes the order.
- Cancelling/expiration refunds only the remaining frozen `totalPrice`.

Right detail panel for DEMAND:

- item icon/name
- remaining requested quantity
- unit price
- supplier matching inventory count
- selected delivery quantity
- payout for selected quantity
- requester
- expiration
- deliver button

## Demand delivery goes directly to mailbox

New demand fulfillment no longer uses the normal `delivered -> requester confirms -> inventory insert` flow.

On successful fulfillment:

1. preflight supplier inventory and supplier balance limit;
2. preflight requester market-mail / DeliveryBox capacity;
3. atomically reserve/decrement the authoritative demand order;
4. remove q matching items from supplier with rollback token;
5. create one MARKET mailbox business delivery for this fulfillment, splitting the physical quantity into native stack-sized attachments;
6. credit supplier by `unitPrice * q`;
7. commit and notify requester if online.

Mail semantics:

- type: MARKET
- source: stable market demand source containing/associated with tradeId
- subject fallback: `求购订单已交付`
- body should expose supplier, delivered quantity, unit price, paid amount, and remaining demand quantity
- attachments are the actual delivered items
- online requester receives existing mailbox toast/sound
- offline requester receives it normally on next login

If any authoritative stage fails, compensate all previous mutations. Never leave a paid supplier with no buyer attachment, or consume supplier items without payout/order transition.

### Mail capacity

Market settlement is critical economic mail. Do not silently orphan DeliveryBox entries.

Preferred policy:

- reserve some mailbox capacity from player-to-player mail, so normal player mail cannot consume the entire hard mailbox limit;
- MARKET / COMPENSATION / other critical system mail may use the reserved tail capacity;
- if even critical capacity is exhausted, fulfillment fails before supplier inventory/payment mutation.

## Legacy delivered demand orders

Keep read compatibility for existing `delivered=true` demand records.

- Existing delivered orders may continue to use the old confirm path until cleared.
- New demand fulfillment should never create `delivered=true`; it should complete directly through mailbox settlement.
- Once legacy data is naturally gone, the confirm-demand UI/action can be deprecated in a later major data migration.

## Live refresh

Market mutations from any player must invalidate open market pages.

- Broadcast a lightweight market revision/invalidation signal after successful create/fill/cancel/expire/admin-remove transitions.
- Open market screen silently re-requests its current query using the same filter/search/sort.
- Preserve selected tradeId when it still exists.
- If selected order remains but quantity/price/state changed, refresh the right pane in place.
- If selected order disappeared, show a brief `订单已失效/已成交` state then clear selection.
- Preserve page when possible; if selected tradeId moves pages due to sorting/new orders, follow it or clamp page deterministically.

## Search/filter/sort state

Query state should include:

- filter: ALL / SALES / DEMAND / MINE
- query string
- sort
- page/offset

Changing filter/query/sort clears current selection and requests page 0.

## Tests

Required common tests:

- server-side sort before pagination
- deterministic sort tie breakers
- partial sales purchase success
- partial sales exact remainder update
- concurrent/order-changed protection
- partial sales payment/inventory/order rollback
- non-divisible legacy order whole-only behavior
- partial demand fulfillment success
- partial demand remaining escrow/order update
- demand mailbox failure leaves supplier inventory/payment/order unchanged or fully compensated
- final demand fulfillment removes order
- cancel/expiration refunds only remaining value
- legacy delivered demand remains confirmable
- live refresh preserves query/sort/selection when possible
- selected order disappearance clears safely

Target parity:

- Forge 1.20.1
- NeoForge 1.21.1

Do not resurrect deprecated packet architecture; extend the current protocol/service/adaptor structure and keep server authority.

## Implementation Status (2026-08-23)

Implemented in the current `bridge` working tree:

- [x] Stable server-side sorting before pagination (`DEFAULT`, unit-price asc/desc, newest, expiring soon) with deterministic tie-breakers.
- [x] Integer unit-price pricing helper with legacy non-divisible orders remaining whole-order-only.
- [x] New SALES and DEMAND creation intents carry `unitPrice`; the server derives `totalPrice = quantity * unitPrice` with overflow rejection.
- [x] SALES partial fills use exact-order CAS transitions, preserve the same tradeId for remainders, and expose exact rollback tokens.
- [x] DEMAND partial fills reduce remaining quantity and escrow value atomically and never create new `delivered=true` records.
- [x] New DEMAND settlement flow is ordered as preflight -> order CAS reservation -> supplier item removal -> staged MARKET mailbox write -> supplier credit -> notification commit.
- [x] DEMAND mailbox rollback only removes the exact unchanged MailRecord and exact still-unclaimed attachment IDs; uncertain rollback state is surfaced instead of blindly compensating.
- [x] Demand settlement mail uses `MailType.MARKET`, a tradeId-linked source, subject `求购订单已交付`, settlement facts in the body, and native-stack-sized attachments.
- [x] PLAYER mail/DeliveryBox soft caps reserve critical tail capacity for MARKET / COMPENSATION / SYSTEM economic mail while retaining existing physical hard limits for old saves.
- [x] Expired MARKET attachment return no longer silently leaves orphan DeliveryBox entries when mail metadata persistence fails.
- [x] Player self-removal of SALES orders still requires real inventory capacity, while an administrator removing another player's SALES order returns the remaining items through protected MARKET mail (including offline owners); known mailbox failures restore the order and uncertain post-write state never blindly restores it.
- [x] Market confirmation warning text now uses the semantic error/red color instead of secondary gray.
- [x] Market live refresh preserves filter/query/sort and can ask the server to follow the selected tradeId to its new sorted page; invalid/finished orders briefly enter a disabled `订单已失效或已成交` detail state before clearing.
- [x] Page offsets are deterministically clamped when the current page disappears after concurrent market mutations.
- [x] Legacy `delivered=true` demand records remain compatible with the old confirm path.
- [x] Common tests were added/updated for partial fills, rollback, unit-price overflow, remaining escrow refund, critical mailbox capacity, staged demand mail, focus-page refresh, and invalidated selection recovery.
- [x] Forge 1.20.1 and NeoForge 1.21.1 request/message adapters were updated in parallel for quantity, sort/focus, unit-price creation and demand mailbox settlement.

Verification note:

- [x] Forge 1.20.1 full suite: 1002 tests, 0 failures, 1 skipped.
- [x] NeoForge 1.21.1 full suite: 1067 tests, 0 failures, 1 skipped.
- [x] `buildAllTargets --no-daemon --rerun-tasks`: successful, 19 tasks executed.
- [x] Common production sources remain loader-neutral.
- [x] Code checkpoint: see git history (`complete market v2 mailbox and ui2 integration`).
- [x] `.ai-bridge/` remains local and uncommitted.
