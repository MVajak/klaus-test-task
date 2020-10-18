package klaus.demo.ticket.service;

import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import klaus.demo.*;
import klaus.demo.ticket.domain.CategoryScore;
import klaus.demo.ticket.domain.TicketScore;
import klaus.demo.ticket.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.time.temporal.ChronoUnit.MONTHS;
import static java.util.stream.Collectors.groupingBy;

@Service
public class TicketServiceImpl extends TicketServiceGrpc.TicketServiceImplBase {
    public TicketServiceImpl(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    private final TicketRepository ticketRepository;

    @Override
    public void getScoresByTickets(DateRange request, StreamObserver<TicketValuesResponse> responseObserver) {
        LocalDateTime periodFrom = LocalDateTime.ofEpochSecond(request.getPeriodFrom().getSeconds(), 0, ZoneOffset.UTC);
        LocalDateTime periodTo = LocalDateTime.ofEpochSecond(request.getPeriodTo().getSeconds(), 0, ZoneOffset.UTC);

        ticketRepository.getCategoryScoresOfTicketsBetweenDates(periodFrom, periodTo).stream()
                .collect(groupingBy(TicketScore::getTicketId, groupingBy(TicketScore::getCategoryName)))
                .forEach((ticketId, categoriesScores) -> {
                    Set<CategoryScoreValue> categoryScoreValues = new HashSet<>();
                    categoriesScores.forEach((categoryName, ticketScores) -> {
                        CategoryScoreValue categoryScoreValue = CategoryScoreValue.newBuilder()
                                .setCategoryName(categoryName)
                                .setScore(ticketScores.stream().mapToInt(TicketScore::getScore).sum() / ticketScores.size())
                                .build();
                        categoryScoreValues.add(categoryScoreValue);
                    });
                    TicketValuesResponse response = TicketValuesResponse.newBuilder()
                            .setTicketId(ticketId)
                            .addAllCategoryScores(categoryScoreValues)
                            .build();
                    responseObserver.onNext(response);

                });

        responseObserver.onCompleted();
    }

    @Override
    public void getAggregatedCategories(DateRange request, StreamObserver<CategoryResultResponse> responseObserver) {
        LocalDateTime periodFrom = LocalDateTime.ofEpochSecond(request.getPeriodFrom().getSeconds(), 0, ZoneOffset.UTC);
        LocalDateTime periodTo = LocalDateTime.ofEpochSecond(request.getPeriodTo().getSeconds(), 0, ZoneOffset.UTC);
        boolean isGreaterThanOneMonth = periodFrom.plus(1, MONTHS).isBefore(periodTo);

        if (isGreaterThanOneMonth) {
            WeekFields weekFields = WeekFields.of(LocalDate.MAX.getDayOfWeek(), 7);
            List<CategoryScore> categoryScoreBetweenDates = ticketRepository.getCategoryScoresBetweenDates(periodFrom, periodTo);
            categoryScoreBetweenDates.stream()
                    .collect(groupingBy(CategoryScore::getCategoryName, groupingBy(categoryScore -> categoryScore.getParsedDate().toLocalDate().atStartOfDay().with(DayOfWeek.MONDAY).get(weekFields.weekOfYear()))))
                    .forEach((categoryName, weekdayListMap) -> {
                        Set<DateScore> dateScores = new HashSet<>();
                        Set<CategoryScore> categoryScores = new HashSet<>();
                        weekdayListMap.forEach((weekOfDay, scores) -> {
                            Timestamp timestamp = Timestamp.newBuilder()
                                    .setSeconds(scores.get(0).getParsedDate().toLocalDate().atStartOfDay().with(DayOfWeek.MONDAY).toEpochSecond(ZoneOffset.UTC))
                                    .build();
                            DateScore dateScore = DateScore.newBuilder()
                                    .setDate(timestamp)
                                    .setScore(scores.stream().mapToInt(CategoryScore::getScore).sum() / scores.size())
                                    .build();
                            categoryScores.addAll(scores);
                            dateScores.add(dateScore);
                        });
                        CategoryResultResponse response = CategoryResultResponse.newBuilder()
                                .setCategoryName(categoryName)
                                .setRatingsCount(categoryScores.size())
                                .addAllDateScores(dateScores)
                                .setTotalScore(dateScores.stream().mapToInt(DateScore::getScore).sum() / dateScores.size())
                                .build();
                        responseObserver.onNext(response);
                    });
        } else {
            List<CategoryScore> categoryScoreBetweenDates = ticketRepository.getCategoryScoresBetweenDates(periodFrom, periodTo);
            categoryScoreBetweenDates.stream()
                    .collect(groupingBy(CategoryScore::getCategoryName, groupingBy(categoryScore -> categoryScore.getParsedDate().toLocalDate())))
                    .forEach((categoryName, dateScoresMap) -> {
                        Set<DateScore> dateScores = new HashSet<>();
                        Set<CategoryScore> categoryScores = new HashSet<>();
                        dateScoresMap.forEach((localDate, scores) -> {
                            Timestamp timestamp = Timestamp.newBuilder()
                                    .setSeconds(localDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC))
                                    .build();
                            DateScore dateScore = DateScore.newBuilder()
                                    .setDate(timestamp)
                                    .setScore(scores.stream().mapToInt(CategoryScore::getScore).sum()/scores.size())
                                    .build();
                            categoryScores.addAll(scores);
                            dateScores.add(dateScore);
                        });
                        CategoryResultResponse response = CategoryResultResponse.newBuilder()
                                .setCategoryName(categoryName)
                                .setRatingsCount(categoryScores.size())
                                .addAllDateScores(dateScores)
                                .setTotalScore(dateScores.stream().mapToInt(DateScore::getScore).sum() / dateScores.size())
                                .build();
                        responseObserver.onNext(response);
                    });
        }

        responseObserver.onCompleted();
    }

    @Override
    public void getOverallQualityScore(DateRange request, StreamObserver<QualityResponse> responseObserver) {
        LocalDateTime periodFrom = LocalDateTime.ofEpochSecond(request.getPeriodFrom().getSeconds(), 0, ZoneOffset.UTC);
        LocalDateTime periodTo = LocalDateTime.ofEpochSecond(request.getPeriodTo().getSeconds(), 0, ZoneOffset.UTC);
        List<Integer> scoresBetweenDates = ticketRepository.getScoresBetweenDates(periodFrom, periodTo);
        int sum = scoresBetweenDates.stream().mapToInt(Integer::intValue).sum();
        int score = scoresBetweenDates.isEmpty() ? 0 : sum / scoresBetweenDates.size();

        QualityResponse response = QualityResponse.newBuilder()
                .setScore(score)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getPeriodOverPeriodScoreChange(DoubleDateRange request, StreamObserver<PeriodChangeResponse> responseObserver) {
        LocalDateTime previousPeriodFrom = LocalDateTime.ofEpochSecond(request.getPreviousPeriod().getPeriodFrom().getSeconds(), 0, ZoneOffset.UTC);
        LocalDateTime previousPeriodTo = LocalDateTime.ofEpochSecond(request.getPreviousPeriod().getPeriodTo().getSeconds(), 0, ZoneOffset.UTC);
        LocalDateTime selectedPeriodFrom = LocalDateTime.ofEpochSecond(request.getSelectedPeriod().getPeriodFrom().getSeconds(), 0, ZoneOffset.UTC);
        LocalDateTime selectedPeriodTo = LocalDateTime.ofEpochSecond(request.getSelectedPeriod().getPeriodTo().getSeconds(), 0, ZoneOffset.UTC);

        List<Integer> previousPeriodScores = ticketRepository.getScoresBetweenDates(previousPeriodFrom, previousPeriodTo);
        List<Integer> selectedPeriodScores = ticketRepository.getScoresBetweenDates(selectedPeriodFrom, selectedPeriodTo);

        int previousPeriodPercentage = previousPeriodScores.isEmpty() ? 0 : previousPeriodScores.stream().mapToInt(Integer::intValue).sum() / previousPeriodScores.size();
        int selectedPeriodPercentage = selectedPeriodScores.isEmpty() ? 0 : selectedPeriodScores.stream().mapToInt(Integer::intValue).sum() / selectedPeriodScores.size();
        double increase = (double)selectedPeriodPercentage - previousPeriodPercentage;
        int percentageChange = 0;
        if (selectedPeriodPercentage != 0) {
            percentageChange = (int)((increase / selectedPeriodPercentage) * 100);
        } else if (previousPeriodPercentage != 0) {
            percentageChange = (int)((increase / previousPeriodPercentage) * 100);
        }

        PeriodChangeResponse response = PeriodChangeResponse.newBuilder().setPeriodChangeScore(percentageChange).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
