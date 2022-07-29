package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HabrCareerParse {
    private static final int LAST = 5;
    private static final String SOURCE_LINK = "https://career.habr.com";
    private static final String PAGE_LINK = String
            .format("%s/vacancies/java_developer?page=", SOURCE_LINK);

    private final DateTimeParser dateTimeParser;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    private static String retrieveDescription(String link) throws IOException {
        Connection connection = Jsoup.connect(link);
        Document document = connection.get();
        Element descElement = document.selectFirst(".style-ugc");
        return descElement.text();
    }

    private Post parsing(Element row) throws IllegalArgumentException {
        Element titleElement = row.select(".vacancy-card__title").first();
        Element linkElement = titleElement.child(0);
        Element timeElement = row.select(".vacancy-card__date").first();
        Element dateElement = timeElement.child(0);

        String vacancyName = titleElement.text();
        String vacancyDate = dateElement.attr("datetime");
        String link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
        LocalDateTime dateTime = dateTimeParser.parse(vacancyDate);
        System.out.printf("%s %s %s%n ", vacancyDate, vacancyName, link);

        String vacancyDesc = null;
        try {
            vacancyDesc = retrieveDescription(link);
        } catch (IOException e) {
            throw new IllegalArgumentException();
        }
        System.out.println("< " + vacancyDesc + " >");
        return new Post(titleElement.text(), link, vacancyDesc, dateTime);
    }

    public List<Post> list(String addr) {
        List<Post> posts = new ArrayList<>();
        for (int i = 1; i <= LAST; i++) {
            String pageLink = String.format("%s%s", SOURCE_LINK, addr);
            Connection connection = Jsoup.connect(pageLink + i);
            try {
                Document document = connection.get();
                Elements rows = document.select(".vacancy-card__inner");
                rows.forEach(row -> posts.add(parsing(row)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return posts;
    }

    public static void main(String[] args) {
        HabrCareerParse habrCareerParse = new HabrCareerParse(new HabrCareerDateTimeParser());
        habrCareerParse.list(PAGE_LINK).forEach(System.out::println);
    }

}