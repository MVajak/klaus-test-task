package klaus.demo.ticket.repository;

import klaus.demo.ticket.domain.CategoryScore;
import klaus.demo.ticket.domain.TicketScore;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static klaus.demo.util.ResourceReader.getResourceAsString;
import static org.springframework.jdbc.core.BeanPropertyRowMapper.newInstance;

@Repository
public class TicketRepository {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public TicketRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public List<CategoryScore> getCategoryScoresBetweenDates(LocalDateTime dateFrom, LocalDateTime dateTo) {
        return namedParameterJdbcTemplate.query(
                getResourceAsString("sql/select_category_scores_between_dates.sql", this.getClass()),
                new MapSqlParameterSource()
                        .addValue("dateFrom", dateFrom.toString())
                        .addValue("dateTo", dateTo.toString()),
                newInstance(CategoryScore.class));
    }

    public List<Integer> getScoresBetweenDates(LocalDateTime dateFrom, LocalDateTime dateTo) {
        return namedParameterJdbcTemplate.query(
                getResourceAsString("sql/select_ratings_between_dates.sql", this.getClass()),
                new MapSqlParameterSource()
                        .addValue("dateFrom", dateFrom.toString())
                        .addValue("dateTo", dateTo.toString()), SingleColumnRowMapper.newInstance(Integer.class) );
    }

    public List<TicketScore> getCategoryScoresOfTicketsBetweenDates(LocalDateTime dateFrom, LocalDateTime dateTo) {
        return namedParameterJdbcTemplate.query(
                getResourceAsString("sql/select_category_scores_of_tickets.sql", this.getClass()),
                new MapSqlParameterSource()
                        .addValue("dateFrom", dateFrom.toString())
                        .addValue("dateTo", dateTo.toString()),
                newInstance(TicketScore.class));
    }

}
