package klaus.demo.ticket.service;

import com.google.protobuf.Timestamp;
import io.grpc.internal.testing.StreamRecorder;
import klaus.demo.*;
import klaus.demo.ticket.domain.CategoryScore;
import klaus.demo.ticket.domain.TicketScore;
import klaus.demo.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(SpringExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private final static String FIRST_CATEGORY = "HELLO";
    private final static String SECOND_CATEGORY = "WORLD";

    @Test
    void shouldReturnScoresByTickets() throws Exception {
        LocalDateTime periodFrom = LocalDateTime.parse("2019-07-17T15:49:14");
        LocalDateTime periodTo = LocalDateTime.parse("2019-08-17T15:49:14");
        DateRange request = getDateRange(periodFrom, periodTo);
        TicketScore tickerScore1 = getTickerScore(FIRST_CATEGORY, 10, 123L);
        TicketScore tickerScore2 = getTickerScore(FIRST_CATEGORY, 30, 123L);
        TicketScore tickerScore3 = getTickerScore(SECOND_CATEGORY, 10, 123L);
        TicketScore tickerScore4 = getTickerScore(FIRST_CATEGORY, 20, 1234L);
        doReturn(List.of(tickerScore1, tickerScore2, tickerScore3, tickerScore4)).when(ticketRepository).getCategoryScoresOfTicketsBetweenDates(periodFrom, periodTo);

        StreamRecorder<TicketValuesResponse> responseObserver = StreamRecorder.create();
        ticketService.getScoresByTickets(request, responseObserver);
        if (!responseObserver.awaitCompletion(5, TimeUnit.SECONDS)) {
            fail("The call did not terminate in time");
        }
        assertNull(responseObserver.getError());
        List<TicketValuesResponse> results = responseObserver.getValues();
        assertEquals(2, results.size());
        TicketValuesResponse firstTicketScore = results.stream().filter(response -> response.getTicketId() == 1234L).findFirst().orElseThrow();
        TicketValuesResponse secondTicketScore = results.stream().filter(response -> response.getTicketId() == 123L).findFirst().orElseThrow();

        assertEquals(1, firstTicketScore.getCategoryScoresList().size());
        assertEquals(20, firstTicketScore.getCategoryScoresList().get(0).getScore());
        assertEquals(FIRST_CATEGORY, firstTicketScore.getCategoryScoresList().get(0).getCategoryName());

        assertEquals(2, secondTicketScore.getCategoryScoresList().size());
        assertTrue(secondTicketScore.getCategoryScoresList().stream().anyMatch(s -> s.getScore() == 20));
        assertTrue(secondTicketScore.getCategoryScoresList().stream().anyMatch(s -> s.getScore() == 10));
        assertTrue(secondTicketScore.getCategoryScoresList().stream().anyMatch(s -> FIRST_CATEGORY.equals(s.getCategoryName())));
        assertTrue(secondTicketScore.getCategoryScoresList().stream().anyMatch(s -> SECOND_CATEGORY.equals(s.getCategoryName())));
    }

    @Test
    void shouldReturnDailyCategoryScoreValues() throws Exception {
        LocalDateTime periodFrom = LocalDateTime.parse("2019-07-17T15:49:14");
        LocalDateTime periodTo = LocalDateTime.parse("2019-08-17T15:49:14");
        DateRange request = getDateRange(periodFrom, periodTo);
        CategoryScore categoryScore1 = getCategoryScore(FIRST_CATEGORY, 10, "2019-07-17T15:50:14");
        CategoryScore categoryScore2 = getCategoryScore(FIRST_CATEGORY, 20, "2019-07-17T17:49:14");
        CategoryScore categoryScore3 = getCategoryScore(SECOND_CATEGORY, 30, "2019-07-18T15:49:14");
        CategoryScore categoryScore4 = getCategoryScore(SECOND_CATEGORY, 30, "2019-07-19T15:49:14");
        doReturn(List.of(categoryScore1, categoryScore2, categoryScore3, categoryScore4)).when(ticketRepository).getCategoryScoresBetweenDates(periodFrom, periodTo);

        StreamRecorder<CategoryResultResponse> responseObserver = StreamRecorder.create();
        ticketService.getAggregatedCategories(request, responseObserver);
        if (!responseObserver.awaitCompletion(5, TimeUnit.SECONDS)) {
            fail("The call did not terminate in time");
        }
        assertNull(responseObserver.getError());
        List<CategoryResultResponse> results = responseObserver.getValues();
        assertEquals(2, results.size());
        CategoryResultResponse firstResponse = results.stream().filter(response -> response.getCategoryName().equals(FIRST_CATEGORY)).findFirst().orElseThrow();
        CategoryResultResponse secondResponse = results.stream().filter(response -> response.getCategoryName().equals(SECOND_CATEGORY)).findFirst().orElseThrow();

        assertEquals(2, firstResponse.getRatingsCount());
        assertEquals(1, firstResponse.getDateScoresCount());
        assertEquals(15, firstResponse.getDateScoresList().get(0).getScore());
        assertEquals(categoryScore1.getParsedDate().toLocalDate().atStartOfDay().toEpochSecond(ZoneOffset.UTC), firstResponse.getDateScoresList().get(0).getDate().getSeconds());

        assertEquals(2, secondResponse.getRatingsCount());
        assertEquals(2, secondResponse.getDateScoresCount());
        assertEquals(30, secondResponse.getDateScoresList().get(0).getScore());
        assertEquals(30, secondResponse.getDateScoresList().get(1).getScore());
        assertTrue(secondResponse.getDateScoresList().stream().anyMatch(d -> d.getDate().getSeconds() == categoryScore3.getParsedDate().toLocalDate().atStartOfDay().toEpochSecond(ZoneOffset.UTC)));
        assertTrue(secondResponse.getDateScoresList().stream().anyMatch(d -> d.getDate().getSeconds() == categoryScore4.getParsedDate().toLocalDate().atStartOfDay().toEpochSecond(ZoneOffset.UTC)));
    }

    @Test
    void shouldReturnWeeklyCategoryScoreValues() throws Exception {
        LocalDateTime periodFrom = LocalDateTime.parse("2019-07-17T15:49:14");
        LocalDateTime periodTo = LocalDateTime.parse("2019-08-18T15:49:14");
        DateRange request = getDateRange(periodFrom, periodTo);
        CategoryScore categoryScore1 = getCategoryScore(FIRST_CATEGORY, 10, "2019-07-17T15:50:14");
        CategoryScore categoryScore2 = getCategoryScore(FIRST_CATEGORY, 20, "2019-07-26T17:49:14");
        CategoryScore categoryScore3 = getCategoryScore(SECOND_CATEGORY, 30, "2019-08-13T15:49:14");
        CategoryScore categoryScore4 = getCategoryScore(SECOND_CATEGORY, 30, "2019-08-13T15:49:14");
        doReturn(List.of(categoryScore1, categoryScore2, categoryScore3, categoryScore4)).when(ticketRepository).getCategoryScoresBetweenDates(periodFrom, periodTo);

        StreamRecorder<CategoryResultResponse> responseObserver = StreamRecorder.create();
        ticketService.getAggregatedCategories(request, responseObserver);
        if (!responseObserver.awaitCompletion(5, TimeUnit.SECONDS)) {
            fail("The call did not terminate in time");
        }
        assertNull(responseObserver.getError());
        List<CategoryResultResponse> results = responseObserver.getValues();
        assertEquals(2, results.size());
        CategoryResultResponse firstResponse = results.stream().filter(response -> response.getCategoryName().equals(FIRST_CATEGORY)).findFirst().orElseThrow();
        CategoryResultResponse secondResponse = results.stream().filter(response -> response.getCategoryName().equals(SECOND_CATEGORY)).findFirst().orElseThrow();

        assertEquals(2, firstResponse.getRatingsCount());
        assertEquals(2, firstResponse.getDateScoresCount());
        assertTrue(firstResponse.getDateScoresList().stream().anyMatch(dateScore -> dateScore.getScore() == 10));
        assertTrue(firstResponse.getDateScoresList().stream().anyMatch(dateScore -> dateScore.getScore() == 20));
        assertTrue(firstResponse.getDateScoresList().stream().anyMatch(dateScore -> dateScore.getDate().getSeconds() == categoryScore1.getParsedDate().toLocalDate().atStartOfDay().with(DayOfWeek.MONDAY).toEpochSecond(ZoneOffset.UTC)));
        assertTrue(firstResponse.getDateScoresList().stream().anyMatch(dateScore -> dateScore.getDate().getSeconds() == categoryScore2.getParsedDate().toLocalDate().atStartOfDay().with(DayOfWeek.MONDAY).toEpochSecond(ZoneOffset.UTC)));

        assertEquals(2, secondResponse.getRatingsCount());
        assertEquals(1, secondResponse.getDateScoresCount());
        assertEquals(30, secondResponse.getDateScoresList().get(0).getScore());
        assertEquals(categoryScore3.getParsedDate().toLocalDate().atStartOfDay().with(DayOfWeek.MONDAY).toEpochSecond(ZoneOffset.UTC), secondResponse.getDateScoresList().get(0).getDate().getSeconds());
    }

    @Test
    void shouldReturnOverallQualityScore() throws Exception {
        LocalDateTime periodFrom = LocalDateTime.parse("2019-07-17T15:49:14");
        LocalDateTime periodTo = LocalDateTime.parse("2019-08-17T15:49:14");
        DateRange request = getDateRange(periodFrom, periodTo);

        doReturn(List.of(10, 10)).when(ticketRepository).getScoresBetweenDates(periodFrom, periodTo);

        StreamRecorder<QualityResponse> responseObserver = StreamRecorder.create();
        ticketService.getOverallQualityScore(request, responseObserver);
        if (!responseObserver.awaitCompletion(5, TimeUnit.SECONDS)) {
            fail("The call did not terminate in time");
        }
        assertNull(responseObserver.getError());
        List<QualityResponse> results = responseObserver.getValues();
        assertEquals(1, results.size());
        QualityResponse response = results.get(0);

        assertEquals(10, response.getScore());
    }

    @Test
    void shouldReturnOverallQualityScoreZeroWhenNoScoresFound() throws Exception {
        LocalDateTime periodFrom = LocalDateTime.parse("2019-07-17T15:49:14");
        LocalDateTime periodTo = LocalDateTime.parse("2019-08-17T15:49:14");
        DateRange request = getDateRange(periodFrom, periodTo);

        doReturn(emptyList()).when(ticketRepository).getScoresBetweenDates(periodFrom, periodTo);

        StreamRecorder<QualityResponse> responseObserver = StreamRecorder.create();
        ticketService.getOverallQualityScore(request, responseObserver);
        if (!responseObserver.awaitCompletion(5, TimeUnit.SECONDS)) {
            fail("The call did not terminate in time");
        }
        assertNull(responseObserver.getError());
        List<QualityResponse> results = responseObserver.getValues();
        assertEquals(1, results.size());
        QualityResponse response = results.get(0);

        assertEquals(0, response.getScore());
    }

    @Test
    void shouldReturnPeriodOverPeriodScoreChange() throws Exception {
        LocalDateTime previousPeriodFrom = LocalDateTime.parse("2019-07-17T15:49:14");
        LocalDateTime previousPeriodTo = LocalDateTime.parse("2019-08-17T15:49:14");
        LocalDateTime selectedPeriodFrom = LocalDateTime.parse("2019-10-17T15:49:14");
        LocalDateTime selectedPeriodTo = LocalDateTime.parse("2019-11-17T15:49:14");
        DateRange previousPeriod = getDateRange(previousPeriodFrom, previousPeriodTo);
        DateRange selectedPeriod = getDateRange(selectedPeriodFrom, selectedPeriodTo);
        DoubleDateRange request = DoubleDateRange.newBuilder()
                .setPreviousPeriod(previousPeriod)
                .setSelectedPeriod(selectedPeriod)
                .build();

        doReturn(List.of(10, 10, 30, 50)).when(ticketRepository).getScoresBetweenDates(previousPeriodFrom, previousPeriodTo);
        doReturn(List.of(10, 30, 30, 50)).when(ticketRepository).getScoresBetweenDates(selectedPeriodFrom, selectedPeriodTo);

        StreamRecorder<PeriodChangeResponse> responseObserver = StreamRecorder.create();
        ticketService.getPeriodOverPeriodScoreChange(request, responseObserver);
        if (!responseObserver.awaitCompletion(5, TimeUnit.SECONDS)) {
            fail("The call did not terminate in time");
        }
        assertNull(responseObserver.getError());
        List<PeriodChangeResponse> results = responseObserver.getValues();
        assertEquals(1, results.size());
        PeriodChangeResponse response = results.get(0);

        assertEquals(16, response.getPeriodChangeScore());
    }

    @Test
    void shouldReturnNegativePercentageWhenPreviousPeriodHasHigherScores() throws Exception {
        LocalDateTime previousPeriodFrom = LocalDateTime.parse("2019-07-17T15:49:14");
        LocalDateTime previousPeriodTo = LocalDateTime.parse("2019-08-17T15:49:14");
        LocalDateTime selectedPeriodFrom = LocalDateTime.parse("2019-10-17T15:49:14");
        LocalDateTime selectedPeriodTo = LocalDateTime.parse("2019-11-17T15:49:14");
        DateRange previousPeriod = getDateRange(previousPeriodFrom, previousPeriodTo);
        DateRange selectedPeriod = getDateRange(selectedPeriodFrom, selectedPeriodTo);
        DoubleDateRange request = DoubleDateRange.newBuilder()
                .setPreviousPeriod(previousPeriod)
                .setSelectedPeriod(selectedPeriod)
                .build();

        doReturn(List.of(10, 50, 73, 50)).when(ticketRepository).getScoresBetweenDates(previousPeriodFrom, previousPeriodTo);
        doReturn(List.of(10, 20, 30, 50)).when(ticketRepository).getScoresBetweenDates(selectedPeriodFrom, selectedPeriodTo);

        StreamRecorder<PeriodChangeResponse> responseObserver = StreamRecorder.create();
        ticketService.getPeriodOverPeriodScoreChange(request, responseObserver);
        if (!responseObserver.awaitCompletion(5, TimeUnit.SECONDS)) {
            fail("The call did not terminate in time");
        }
        assertNull(responseObserver.getError());
        List<PeriodChangeResponse> results = responseObserver.getValues();
        assertEquals(1, results.size());
        PeriodChangeResponse response = results.get(0);

        assertEquals(-66, response.getPeriodChangeScore());
    }

    @Test
    void shouldReturnNegativePercentageWhenSelectedPeriodHasZeroScores() throws Exception {
        LocalDateTime previousPeriodFrom = LocalDateTime.parse("2019-07-17T15:49:14");
        LocalDateTime previousPeriodTo = LocalDateTime.parse("2019-08-17T15:49:14");
        LocalDateTime selectedPeriodFrom = LocalDateTime.parse("2019-10-17T15:49:14");
        LocalDateTime selectedPeriodTo = LocalDateTime.parse("2019-11-17T15:49:14");
        DateRange previousPeriod = getDateRange(previousPeriodFrom, previousPeriodTo);
        DateRange selectedPeriod = getDateRange(selectedPeriodFrom, selectedPeriodTo);
        DoubleDateRange request = DoubleDateRange.newBuilder()
                .setPreviousPeriod(previousPeriod)
                .setSelectedPeriod(selectedPeriod)
                .build();

        doReturn(List.of(10, 50, 73, 50)).when(ticketRepository).getScoresBetweenDates(previousPeriodFrom, previousPeriodTo);
        doReturn(emptyList()).when(ticketRepository).getScoresBetweenDates(selectedPeriodFrom, selectedPeriodTo);

        StreamRecorder<PeriodChangeResponse> responseObserver = StreamRecorder.create();
        ticketService.getPeriodOverPeriodScoreChange(request, responseObserver);
        if (!responseObserver.awaitCompletion(5, TimeUnit.SECONDS)) {
            fail("The call did not terminate in time");
        }
        assertNull(responseObserver.getError());
        List<PeriodChangeResponse> results = responseObserver.getValues();
        assertEquals(1, results.size());
        PeriodChangeResponse response = results.get(0);

        assertEquals(-100, response.getPeriodChangeScore());
    }

    @Test
    void shouldReturnZeroWhenBothPeriodsHaveZeroScores() throws Exception {
        LocalDateTime previousPeriodFrom = LocalDateTime.parse("2019-07-17T15:49:14");
        LocalDateTime previousPeriodTo = LocalDateTime.parse("2019-08-17T15:49:14");
        LocalDateTime selectedPeriodFrom = LocalDateTime.parse("2019-10-17T15:49:14");
        LocalDateTime selectedPeriodTo = LocalDateTime.parse("2019-11-17T15:49:14");
        DateRange previousPeriod = getDateRange(previousPeriodFrom, previousPeriodTo);
        DateRange selectedPeriod = getDateRange(selectedPeriodFrom, selectedPeriodTo);
        DoubleDateRange request = DoubleDateRange.newBuilder()
                .setPreviousPeriod(previousPeriod)
                .setSelectedPeriod(selectedPeriod)
                .build();

        doReturn(emptyList()).when(ticketRepository).getScoresBetweenDates(previousPeriodFrom, previousPeriodTo);
        doReturn(emptyList()).when(ticketRepository).getScoresBetweenDates(selectedPeriodFrom, selectedPeriodTo);

        StreamRecorder<PeriodChangeResponse> responseObserver = StreamRecorder.create();
        ticketService.getPeriodOverPeriodScoreChange(request, responseObserver);
        if (!responseObserver.awaitCompletion(5, TimeUnit.SECONDS)) {
            fail("The call did not terminate in time");
        }
        assertNull(responseObserver.getError());
        List<PeriodChangeResponse> results = responseObserver.getValues();
        assertEquals(1, results.size());
        PeriodChangeResponse response = results.get(0);

        assertEquals(0, response.getPeriodChangeScore());
    }

    private DateRange getDateRange(LocalDateTime periodFrom, LocalDateTime periodTo) {
        Timestamp periodFromTimestamp = Timestamp.newBuilder()
                .setSeconds(periodFrom.toEpochSecond(ZoneOffset.UTC))
                .build();
        Timestamp periodToTimestamp = Timestamp.newBuilder()
                .setSeconds(periodTo.toEpochSecond(ZoneOffset.UTC))
                .build();

        return DateRange.newBuilder()
                .setPeriodFrom(periodFromTimestamp)
                .setPeriodTo(periodToTimestamp)
                .build();
    }

    private TicketScore getTickerScore(String categoryName, int score, Long ticketId) {
        TicketScore ticketScore = new TicketScore();
        ticketScore.setCategoryName(categoryName);
        ticketScore.setScore(score);
        ticketScore.setTicketId(ticketId);
        return ticketScore;
    }

    private CategoryScore getCategoryScore(String categoryName, int score, String createdAt) {
        CategoryScore categoryScore = new CategoryScore();
        categoryScore.setCategoryName(categoryName);
        categoryScore.setScore(score);
        categoryScore.setCreatedAt(createdAt);
        return categoryScore;
    }
}