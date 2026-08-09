package com.mo.economy_system.common.economy;

import com.mo.economy_system.common.network.ShopItemSnapshot;
import java.util.Locale;
import java.util.Objects;

/** Loader-neutral dynamic pricing and purchase-statistics policy. */
public final class ShopPricingPolicy {
  private ShopPricingPolicy() {}

  public enum Mode {
    DEMAND,
    STOCK;

    public static Mode parse(String value) {
      return "stock".equalsIgnoreCase(Objects.requireNonNullElse(value, "")) ? STOCK : DEMAND;
    }

    public String serializedName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  public record Config(
      double minPriceMultiplier,
      double maxPriceMultiplier,
      double maxCycleChangeRate,
      double demandSensitivity,
      double demandDecay,
      double idleReturnRate,
      int defaultMaxStock,
      int minMaxStock,
      double restockRate,
      double stockSensitivity) {
    public Config {
      minPriceMultiplier = clamp(minPriceMultiplier, 0.01D, 100.0D);
      maxPriceMultiplier = Math.max(minPriceMultiplier, clamp(maxPriceMultiplier, 0.01D, 100.0D));
      maxCycleChangeRate = clamp(maxCycleChangeRate, 0.0D, 1.0D);
      demandSensitivity = clamp(demandSensitivity, 0.0D, 10.0D);
      demandDecay = clamp(demandDecay, 0.0D, 1.0D);
      idleReturnRate = clamp(idleReturnRate, 0.0D, 1.0D);
      defaultMaxStock = Math.max(1, defaultMaxStock);
      minMaxStock = Math.max(1, minMaxStock);
      restockRate = clamp(restockRate, 0.0D, 1.0D);
      stockSensitivity = clamp(stockSensitivity, 0.0D, 10.0D);
    }

    public static Config defaults() {
      return new Config(0.5D, 3.0D, 0.08D, 0.035D, 0.65D, 0.04D,
          512, 64, 0.18D, 1.8D);
    }
  }

  public static ShopItemSnapshot initialize(ShopItemSnapshot item, Config config) {
    Objects.requireNonNull(item, "item");
    Objects.requireNonNull(config, "config");
    int expectedMax = expectedMaxStock(item.basePrice(), config);
    int maxStock = item.maxVirtualStock() <= 0 ? expectedMax : item.maxVirtualStock();
    int stock = item.virtualStock() <= 0 ? maxStock : item.virtualStock();
    stock = Math.max(0, Math.min(stock, maxStock));
    return withPricing(item, item.currentPrice(), item.lastPrice(), item.fluctuationFactor(),
        Math.max(0, item.recentDemand()), stock, maxStock);
  }

  public static ShopItemSnapshot recordPurchase(ShopItemSnapshot item, int quantity, Config config) {
    Objects.requireNonNull(item, "item");
    if (quantity <= 0) return item;
    ShopItemSnapshot initialized = initialize(item, config);
    int demand = saturatedAdd(initialized.recentDemand(), quantity);
    int stock = (int) Math.max(0L, (long) initialized.virtualStock() - quantity);
    return withPricing(initialized, initialized.currentPrice(), initialized.lastPrice(),
        initialized.fluctuationFactor(), demand, stock, initialized.maxVirtualStock());
  }

  public static ShopItemSnapshot adjust(ShopItemSnapshot item, Config config, Mode mode) {
    Objects.requireNonNull(item, "item");
    Objects.requireNonNull(config, "config");
    Objects.requireNonNull(mode, "mode");
    if (item.basePrice() <= 0) return item;

    ShopItemSnapshot initialized = initialize(item, config);
    int currentPrice = Math.max(1, initialized.currentPrice());
    int targetPrice = mode == Mode.STOCK
        ? stockTargetPrice(initialized, config)
        : demandTargetPrice(initialized, config);
    int newPrice = clampPrice(moveToward(currentPrice, targetPrice, config.maxCycleChangeRate()),
        initialized.basePrice(), config);
    double fluctuation = roundRate((newPrice - currentPrice) / (double) currentPrice);
    int demand = boundedRound(initialized.recentDemand() * config.demandDecay());
    int missing = Math.max(0, initialized.maxVirtualStock() - initialized.virtualStock());
    int restored = boundedCeil(missing * config.restockRate());
    int stock = (int) Math.min(initialized.maxVirtualStock(),
        (long) initialized.virtualStock() + restored);
    return withPricing(initialized, newPrice, currentPrice, fluctuation, demand, stock,
        initialized.maxVirtualStock());
  }

  private static int demandTargetPrice(ShopItemSnapshot item, Config config) {
    int basePrice = item.basePrice();
    double demandPressure = Math.log1p(item.recentDemand()) * config.demandSensitivity();
    double currentPremium = (item.currentPrice() - (double) basePrice) / basePrice;
    double multiplier = 1.0D + demandPressure - currentPremium * config.idleReturnRate();
    return clampPrice(boundedRound(basePrice * multiplier), basePrice, config);
  }

  private static int stockTargetPrice(ShopItemSnapshot item, Config config) {
    int basePrice = item.basePrice();
    double stockRatio = item.maxVirtualStock() <= 0
        ? 1.0D
        : item.virtualStock() / (double) item.maxVirtualStock();
    double scarcity = 1.0D - clamp(stockRatio, 0.0D, 1.0D);
    double multiplier = 1.0D + Math.pow(scarcity, 1.35D) * config.stockSensitivity();
    return clampPrice(boundedRound(basePrice * multiplier), basePrice, config);
  }

  private static int moveToward(int currentPrice, int targetPrice, double maxCycleChangeRate) {
    int maxStep = Math.max(1, boundedCeil(currentPrice * maxCycleChangeRate));
    long delta = (long) targetPrice - currentPrice;
    if (Math.abs(delta) <= maxStep) return targetPrice;
    return delta > 0 ? saturatedAdd(currentPrice, maxStep) : Math.max(1, currentPrice - maxStep);
  }

  private static int clampPrice(int price, int basePrice, Config config) {
    int min = Math.max(1, boundedFloor(basePrice * config.minPriceMultiplier()));
    int max = Math.max(min, boundedCeil(basePrice * config.maxPriceMultiplier()));
    return Math.max(min, Math.min(max, price));
  }

  private static int expectedMaxStock(int basePrice, Config config) {
    long priceScaled = Math.max(1L, basePrice) * 32L;
    long expected = Math.max(config.minMaxStock(), Math.max((long) config.defaultMaxStock(), priceScaled));
    return (int) Math.min(Integer.MAX_VALUE, expected);
  }

  private static ShopItemSnapshot withPricing(ShopItemSnapshot item, int currentPrice, int lastPrice,
                                               double fluctuation, int demand, int stock, int maxStock) {
    return new ShopItemSnapshot(item.shopItemId(), item.itemId(), item.basePrice(), currentPrice, lastPrice,
        item.description(), fluctuation, item.nbt(), item.itemData(), demand, stock, maxStock);
  }

  private static int saturatedAdd(int left, int right) {
    return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (long) left + right));
  }

  private static int boundedRound(double value) {
    if (!Double.isFinite(value)) return value > 0 ? Integer.MAX_VALUE : 0;
    return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, Math.round(value)));
  }

  private static int boundedCeil(double value) {
    if (!Double.isFinite(value)) return value > 0 ? Integer.MAX_VALUE : 0;
    return (int) Math.max(0.0D, Math.min(Integer.MAX_VALUE, Math.ceil(value)));
  }

  private static int boundedFloor(double value) {
    if (!Double.isFinite(value)) return value > 0 ? Integer.MAX_VALUE : 0;
    return (int) Math.max(0.0D, Math.min(Integer.MAX_VALUE, Math.floor(value)));
  }

  private static double roundRate(double rate) {
    return Math.round(rate * 100.0D) / 100.0D;
  }

  private static double clamp(double value, double min, double max) {
    if (Double.isNaN(value)) return min;
    return Math.max(min, Math.min(max, value));
  }
}
