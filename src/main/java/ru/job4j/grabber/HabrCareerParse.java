package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HabrCareerParse {
    private static final int LAST = 5;
    private static int page;
    private static final String SOURCE_LINK = "https://career.habr.com";
    private static final String PAGE_LINK = String
            .format("%s/vacancies/java_developer?page=%s", SOURCE_LINK, page);

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

    public List<Post> list(String addr) throws IOException {
        List<Post> posts = new ArrayList<>();
        for (int page = 1; page <= LAST; page++) {
            Connection connection = Jsoup.connect(addr + page);
            Document document = connection.get();
            Elements rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> {
                Element titleElement = row.select(".vacancy-card__title").first();
                Element linkElement = titleElement.child(0);
                Element timeElement = row.select(".vacancy-card__date").first();
                Element dateElement = timeElement.child(0);

                String vacancyName = titleElement.text();
                String vacancyDate = dateElement.attr("datetime");
                String link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));

                System.out.printf("%s %s %s%n ", vacancyDate, vacancyName, link);

                String vacancyDesc = null;
                try {
                    vacancyDesc = retrieveDescription(link);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("< " + vacancyDesc + " >");
            });
        }
        return posts;
    }

        public static void main(String[] args) throws IOException {
            HabrCareerParse habrCareerParse = new HabrCareerParse(new HabrCareerDateTimeParser());
            habrCareerParse.list(PAGE_LINK);
        }
    }