package challenge12;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.Double.parseDouble;

public class solution {
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
        public final double localMoney;
        public final double foreignMoney;

        public AccountRecord(int dayNo, double localMoney, double foreignMoney) {
            this.dayNo = dayNo;
            this.localMoney = localMoney;
            this.foreignMoney = foreignMoney;
        }
    }

    public static class CurrencyRate {
        public final int dayNo;
        public final double buy;
        public final double sell;

        public CurrencyRate(int dayNo, double buy, double sell) {
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
                .map(arr -> new CurrencyRate(counter.getAndIncrement(), parseDouble(arr[0]), parseDouble(arr[1])))
                .collect(Collectors.toList());
    }

    private static AccountRecord sellForeign(AccountRecord current, CurrencyRate rate) {
        return new AccountRecord(rate.dayNo,
                current.localMoney + current.foreignMoney * rate.buy,
                0.0);
    }

    private static AccountRecord buyForeign(AccountRecord current, CurrencyRate rate) {
        return new AccountRecord(rate.dayNo,
                0.0,
                current.foreignMoney + (current.localMoney / rate.sell));
    }

    public static <T> T getLast(List<T> list) {
        return list.get(list.size() - 1);
    }

    private static void calculate(int dayNo,
                                  List<CurrencyRate> rates,
                                  List<AccountRecord> history,
                                  Consumer<List<AccountRecord>> checkResult
    ) {
        AccountRecord current = getLast(history);
        AccountRecord first = history.get(0);

        if (dayNo == rates.size()) { // after last day
            if (current.localMoney > first.localMoney) {
                checkResult.accept(history);
            }
            return;
        }
        CurrencyRate dayNoRate = rates.get(dayNo - 1);

        // 3 actions to check: Buy, Cell, Nothing
        if (current.localMoney > 0) {
            AccountRecord afterBuy = buyForeign(current, dayNoRate);
            history.add(afterBuy);
            calculate(dayNo + 1, rates, history, checkResult);
            history.remove(history.size() - 1);
        }
        if (current.foreignMoney > 0) {
            AccountRecord afterSell = sellForeign(current, dayNoRate);
            if (afterSell.localMoney > first.localMoney) {
                history.add(afterSell);
                calculate(dayNo + 1, rates, history, checkResult);
                history.remove(history.size() - 1);
            }
        }
        if ((current.localMoney > 0) || (current.foreignMoney > 0)) {
            calculate(dayNo + 1, rates, history, checkResult);
        }
    }

    private static void prettyPrintLine(int dayNo, List<CurrencyRate> ratesList, AccountRecord acc) {
        String fmt = "| %3s | %14s | %14s | %14s | %14s |%n";
        if (dayNo == 0) {
            System.out.printf(fmt, "Day", "buy", "sell", "Cash", "Foreign");
        }
        String dd, r1, r2, a1, a2;
        if (dayNo > 0 && dayNo < ratesList.size()) {
            dd = String.format("%02d", dayNo);
            r1 = String.format("%8.4f", ratesList.get(dayNo - 1).buy);
            r2 = String.format("%8.4f", ratesList.get(dayNo - 1).sell);
        } else {
            dd = String.format("%2s", " ");
            r1 = String.format("%12s", " ");
            r2 = String.format("%12s", " ");
        }
        if (acc != null) {
            a1 = String.format("%12.2f", acc.localMoney);
            a2 = String.format("%12.2f", acc.foreignMoney);
        } else {
            a1 = String.format("%14s", " ");
            a2 = String.format("%14s", " ");
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

    private static boolean isEqual(Double r1, Double r2) {
        return Math.abs(r1 - r2) < 1e-5;
    }

    private static double solution(String fn) throws IOException {
        List<CurrencyRate> ratesList = readCurrencyRates(fn);
        AccountRecord initialAccount = new AccountRecord(0, 1000.00, 0.00);

        ArrayList<AccountRecord> hist = new ArrayList<>(ratesList.size() + 1);
        hist.add(initialAccount);

        final ArrayList<AccountRecord> best = new ArrayList<>(hist);
        calculate(1, ratesList, hist,
                opChain -> {
                    if ((getLast(opChain).localMoney > getLast(best).localMoney)
                            || (isEqual(getLast(opChain).localMoney, getLast(best).localMoney) && (opChain.size() < best.size()))
                    ){
                        best.clear();
                        best.addAll(opChain);
                    }
                });
        AccountRecord last = getLast(best);
        prettyPrintResult(ratesList, best);

        double r = last.localMoney - initialAccount.localMoney;
        System.out.printf("The potential maximum gain is %.2f%n", r);
        return r;
    }

    public static void main(String[] args) throws Exception {
        solution("./src/challenge12/input_12.csv");
    }
}
