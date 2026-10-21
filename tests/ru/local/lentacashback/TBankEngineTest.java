package ru.local.lentacashback;

import java.time.LocalDate;
import java.util.Arrays;

public final class TBankEngineTest {
    public static void main(String[] args) {
        LocalDate today=LocalDate.of(2026,10,21);
        TBankEngine.Operation start=new TBankEngine.Operation("one","2026-07-14","Магазин","Супермаркеты",12345,true);
        TBankEngine.Report r=TBankEngine.calculate(Arrays.asList(start,start,
                new TBankEngine.Operation("old","2026-07-13","Магазин","Супермаркеты",99999,true),
                new TBankEngine.Operation("transfer","2026-10-21","Перевод","Переводы",10000,true),
                new TBankEngine.Operation("cinema","2026-10-21","Кинотеатр","Кинотеатры",100000,true)),
                Arrays.asList(new TBankEngine.Offer("Все покупки",1,"cashback",true,"",true),
                        new TBankEngine.Offer("Кино",5,"cashback",true,"",true),
                        new TBankEngine.Offer("Автосервисы в Городе",15,"cashback",true,"",true)),today);
        if(r.count!=2 || r.total!=112345 || r.groups.size()!=2)throw new AssertionError("Period, duplicate or non-purchase");
        TBankEngine.Recommendation cinema=r.recommendations.stream().filter(x->x.offer.name.equals("Кино")).findFirst().get();
        if(cinema.spend!=100000 || cinema.estimate()!=5000)throw new AssertionError("Bank category mapping");
        if(r.recommendations.stream().filter(x->x.offer.name.contains("Городе")).findFirst().get().spend!=0)throw new AssertionError("Restricted channel");
        System.out.println("PASS: T-Bank 100 days, duplicate IDs, bank categories, precise totals and restricted service channels.");
    }
}
