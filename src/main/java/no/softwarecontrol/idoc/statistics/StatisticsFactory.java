package no.softwarecontrol.idoc.statistics;

import no.softwarecontrol.idoc.data.entityhelper.ProductionStatistic;
import no.softwarecontrol.idoc.data.entityhelper.WalletProject;
import no.softwarecontrol.idoc.data.entityobject.Observation;
import no.softwarecontrol.idoc.data.entityobject.Project;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class StatisticsFactory {

    public static List<ProductionStatistic> createMonthly(List<WalletProject> walletProjects){

        List<ProductionStatistic> productionStatistics = new ArrayList<>();
        DateTime today = new DateTime(new Date());
        DateTime.Property previousMonth = today.monthOfYear();

        List<WalletProject> sortedProjects = new ArrayList<>(walletProjects);
        Collections.sort(sortedProjects, (WalletProject o1, WalletProject o2) -> o2.createdDate.compareTo(o1.createdDate));
        ProductionStatistic productionStatistic = new ProductionStatistic();
        productionStatistics.add(productionStatistic);
        productionStatistic.setToDate(today.toDate());
        productionStatistic.setFromDate(today.dayOfMonth().withMinimumValue().withMillisOfDay(0).toDate());
        DateTime.Property thisMonth = new DateTime(new Date()).monthOfYear();
        productionStatistic.setPeriodName(thisMonth.getAsText() + " " + today.year().getAsText());
        DateTime createDateTime = null;
        for(WalletProject walletProject:sortedProjects){
            createDateTime =  new DateTime(walletProject.createdDate);
            thisMonth = new DateTime(createDateTime).monthOfYear();
            if(!thisMonth.getAsText().equalsIgnoreCase(previousMonth.getAsText())){
                previousMonth = thisMonth;
                productionStatistic = new ProductionStatistic();
                DateTime firstDayOfMonth = createDateTime.dayOfMonth().withMinimumValue();
                firstDayOfMonth = firstDayOfMonth.withMillisOfDay(0);
                productionStatistic.setFromDate(firstDayOfMonth.toDate());
                DateTime lastDayOfMonth = createDateTime.dayOfMonth().withMaximumValue();
                lastDayOfMonth = lastDayOfMonth.withTime(23,59,59,999);

                productionStatistic.setPeriodName(thisMonth.getAsText() + " " + createDateTime.year().getAsText());
                productionStatistic.setToDate(lastDayOfMonth.toDate());
                productionStatistics.add(productionStatistic);
            }
            Double invoicePointPrice = 0.0;
            if(walletProject.pointPrice != null) {
                invoicePointPrice = walletProject.pointPrice;
            } else {
                invoicePointPrice = 240.0;
            }

            Double projectPointCount = walletProject.pointCount;
            if(projectPointCount > walletProject.maxChildren && walletProject.maxChildren != 0) {
                projectPointCount = walletProject.maxChildren;
            }
            int intAssetCounter = (int) Math.round(walletProject.pointCount);
            productionStatistic.setAssetCount(productionStatistic.getAssetCount() + intAssetCounter);
            productionStatistic.setPointCount(productionStatistic.getPointCount() + projectPointCount * walletProject.pointFactor);
            Double discountFactor = 1.0;
            if(walletProject.pointDiscount != null) {
                discountFactor = walletProject.pointDiscount;
            }
            productionStatistic.setRevenue(productionStatistic.getRevenue() + invoicePointPrice * projectPointCount * walletProject.pointFactor * discountFactor);
        }
        return productionStatistics;
    }

    /*public Double getPointCount(Project project)  {
        Double counter = 0.0;
        if (project.getProjectList().isEmpty()) {
            counter = counter + project.getDisipline().getPointPrice();
        } else {
            List<Project> projects = new ArrayList<>(project.getProjectList());
            List<Project> activeChildren = projects.stream().filter(r -> r.isDeleted() == false).collect(Collectors.toList());
            if (activeChildren.size() >= project.getDisipline().getMaxChildren()) {
                counter = counter + (project.getDisipline().getMaxChildren() * project.getDisipline().getPointPrice());
            } else {
                for (Project child: activeChildren) {
                    counter = counter + project.getDisipline().getPointPrice();
                }
            }
        }
        return counter;
    }*/

    private static int countDeviation(Project project, int deviationLevel) {
        int counter = 0;
        for (Observation observation : project.getObservationList()) {
            if (observation.getDeviation() == deviationLevel && !observation.isDeleted()) {
                counter++;
            }
        }
        for(Project child:project.getProjectList()){
            for (Observation observation : child.getObservationList()) {
                if (observation.getDeviation() == deviationLevel && !observation.isDeleted()) {
                    counter++;
                }
            }
        }
        return counter;
    }
}
