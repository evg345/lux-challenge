package challenge12;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class Solution {
    public static int CURRENCY_SCALE = 2;
    public static int FOREIGN_SCALE = 2;
    public static int RATE_SCALE = 0;

    /*
    There is an exchange office that displays every day an exchange rate for a foreign currency. There is a
    buy rate and a sell rate every day.
    You have an amount of the foreign currency (e.g. 1000). Following the exchange rates, find the potential
    maximum gain if you exchange your money in day M from the foreign currency to the local currency,
    then you exchange them back from the local currency to the foreign currency in day N (N > M).
    For example, the list of exchange rates (buy/sell)
            134 140
            132 136
            134 140
            138 142
            132 136
            134 140
            140 142
            132 136
            134 140
            138 142
            132 136
            134 140
            142 144
            140 142
            140 142
            138 142
    The solution will be that you exchange the amount of 1000 from the foreign currency to the local
    currency in day 2 at a rate of 136, then to exchange back from local currency to the foreign currency in
    day 13, at a rate of 142, with a potential gain of 44.1176470588234
     */

    public static class AccountRecord {
        public final int dayNo;
        public final BigDecimal localMoney;
        public final BigDecimal foreignMoney;

        public AccountRecord(int dayNo, BigDecimal localMoney, BigDecimal foreignMoney) {
            this.dayNo = dayNo;
            this.localMoney = localMoney;
            this.foreignMoney = foreignMoney;
        }
    }

    public static class CurrencyRate {
        public final int dayNo;
        public final BigDecimal buy;
        public final BigDecimal sell;

        public CurrencyRate(int dayNo, BigDecimal buy, BigDecimal sell) {
            this.dayNo = dayNo;
            this.buy = buy;
            this.sell = sell;
        }
    }

    private static List<CurrencyRate> readCurrencyRates(String fn) throws IOException {
        AtomicInteger counter = new AtomicInteger(1);
        return Files.readAllLines(Path.of(fn), StandardCharsets.UTF_8).stream()
                .map(s -> s.split("\\s*[, |]\\s*", 3))
                .filter(arr -> arr.length >= 2 && !arr[0].isEmpty())
                .map(arr -> new CurrencyRate(
                        counter.getAndIncrement(),
                        BigDecimal.valueOf(Double.parseDouble(arr[0])),
                        BigDecimal.valueOf(Double.parseDouble(arr[1]))))
                .collect(Collectors.toList());
    }

    private static AccountRecord sellForeign(AccountRecord current, CurrencyRate rate) {
        return new AccountRecord(
                rate.dayNo,
                current.localMoney.add(
                        current.foreignMoney
                                .multiply(rate.buy)
                                .divide(BigDecimal.ONE, CURRENCY_SCALE, RoundingMode.FLOOR)),
                BigDecimal.ZERO
        );
    }

    private static AccountRecord buyForeign(AccountRecord current, CurrencyRate rate) {
        return new AccountRecord(
                rate.dayNo,
                BigDecimal.ZERO,
                current.foreignMoney.add(
                        current.localMoney
                                .divide(rate.sell, FOREIGN_SCALE, RoundingMode.FLOOR))
        );
    }

    public static <T> T getLast(List<T> list) {
        return list.get(list.size() - 1);
    }

    private static void calculate(int dayNo,
                                  List<CurrencyRate> rates,
                                  List<AccountRecord> history,
                                  Consumer<List<AccountRecord>> checkResultFn
    ) {
        AccountRecord current = getLast(history);
        AccountRecord first = history.get(0);

        if (dayNo == rates.size()) { // after last day
            if (current.localMoney.compareTo(first.localMoney) > 0) {
                checkResultFn.accept(history);
            }
            return;
        }
        CurrencyRate dayNoRate = rates.get(dayNo - 1);

        // 3 actions to check: Buy, Cell, Nothing
        if (current.localMoney.compareTo(BigDecimal.ZERO) > 0) {
            AccountRecord afterBuy = buyForeign(current, dayNoRate);
            history.add(afterBuy);
            calculate(dayNo + 1, rates, history, checkResultFn);
            history.remove(history.size() - 1);
        }
        if (current.foreignMoney.compareTo(BigDecimal.ZERO) > 0) {
            AccountRecord afterSell = sellForeign(current, dayNoRate);
            if (afterSell.localMoney.compareTo(first.localMoney) > 0) {
                history.add(afterSell);
                calculate(dayNo + 1, rates, history, checkResultFn);
                history.remove(history.size() - 1);
            }
        }
        if (current.localMoney.compareTo(BigDecimal.ZERO) > 0
                || (current.foreignMoney.compareTo(BigDecimal.ZERO) > 0)) {
            calculate(dayNo + 1, rates, history, checkResultFn);
        }
    }

    private static void prettyPrintLine(int dayNo, List<CurrencyRate> ratesList, AccountRecord acc) {
        String fmt = "| %3s | %14s | %14s | %14s | %14s |%n";
        if (dayNo == 0) {
            System.out.printf(fmt, "Day", "Rate/buy", "Rate/sell", "Cash", "Foreign");
        }
        String dd, r1, r2, a1, a2;
        if (dayNo > 0 && dayNo < ratesList.size()) {
            dd = String.format("%02d", dayNo);
            r1 = String.format("%."+RATE_SCALE+"f", ratesList.get(dayNo - 1).buy);
            r2 = String.format("%."+RATE_SCALE+"f", ratesList.get(dayNo - 1).sell);
        } else {
            dd = String.format("%s", " ");
            r1 = String.format("%s", " ");
            r2 = String.format("%s", " ");
        }
        if (acc != null) {
            a1 = String.format("%."+CURRENCY_SCALE+"f", acc.localMoney);
            a2 = String.format("%."+FOREIGN_SCALE+"f", acc.foreignMoney);
        } else {
            a1 = String.format("%s", " ");
            a2 = String.format("%s", " ");
        }
        System.out.printf(fmt, dd, r1, r2, a1, a2);
    }

    private static void prettyPrintResult(List<CurrencyRate> ratesList, ArrayList<AccountRecord> best) {
        System.out.println("Best variant:");
        AccountRecord last = null;
        for (int i = 0; i < ratesList.size(); i++) {
            AccountRecord acc = null;
            if (!best.isEmpty()) {
                if (best.get(0).dayNo == i) {
                    acc = best.get(0);
                    best.remove(0);
                    last = acc;
                }
            }
            prettyPrintLine(i, ratesList, acc);
        }
        prettyPrintLine(ratesList.size(), ratesList, last);
    }

    public static BigDecimal solution(long startMoney, String fn) throws IOException {
        return solution(startMoney, readCurrencyRates(fn));
    }

    public static BigDecimal solution(long startMoney, List<CurrencyRate> ratesList) {

        AccountRecord initialAccount = new AccountRecord(0, BigDecimal.valueOf(startMoney), BigDecimal.ZERO);

        ArrayList<AccountRecord> hist = new ArrayList<>(ratesList.size() + 1);
        hist.add(initialAccount);

        final ArrayList<AccountRecord> best = new ArrayList<>(hist);
        calculate(1, ratesList, hist,
                opChain -> {
                    if ((getLast(opChain).localMoney.compareTo(getLast(best).localMoney) > 0)
                            || (opChain.size() < best.size() && getLast(opChain).localMoney.compareTo(getLast(best).localMoney) == 0)
                    ) {
                        best.clear();
                        best.addAll(opChain);
                    }
                });
        AccountRecord last = getLast(best);
        prettyPrintResult(ratesList, best);

        return last.localMoney.subtract(initialAccount.localMoney);
    }

    public static void main(String[] args) throws Exception {
        System.out.printf("The potential maximum gain is %."+CURRENCY_SCALE+"f%n",
                solution(1000, "./src/challenge12/input_12.csv")
        );
    }
}
