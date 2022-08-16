package ru.job4j.grabber;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PsqlStore implements Store, AutoCloseable {

    private Connection cnn;

    public PsqlStore(Properties cfg) {
        try {
            Class.forName(cfg.getProperty("jdbc.driver"));
            cnn = DriverManager.getConnection(
                    cfg.getProperty("jdbc.url"),
                    cfg.getProperty("jdbc.username"),
                    cfg.getProperty("jdbc.password")
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void save(Post post) {
        try (PreparedStatement ps = cnn.
                prepareStatement(
                        "insert into post (title, description, link, created) values(?, ?, ?, ?) "
                                + "on conflict (link) do nothing",
                        Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, post.getTitle());
            ps.setString(2, post.getDescription());
            ps.setString(3, post.getLink());
            ps.setTimestamp(4, Timestamp.valueOf(post.getCreated()));
            ps.execute();
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    post.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Post getPost(ResultSet resultSet) throws SQLException {
        return new Post(resultSet.getInt("id"),
                resultSet.getString("title"),
                resultSet.getString("link"),
                resultSet.getString("description"),
                resultSet.getTimestamp("created").toLocalDateTime());
    }

    @Override
    public List<Post> getAll() throws SQLException {
        List<Post> posts = new ArrayList<>();
        try (PreparedStatement statement = cnn.prepareStatement("SELECT * FROM post")) {
            try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                posts.add(getPost(resultSet));
            }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return posts;
        }
    }

    @Override
    public Post findById(int id) throws SQLException {
        Post post = null;
        try (PreparedStatement ps = cnn.
                prepareStatement("SELECT * FROM post WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet resultSet = ps.executeQuery()) {
                if (resultSet.next()) {
                    post = getPost(resultSet);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return post;
        }
    }

    @Override
    public void close() throws Exception {
        if (cnn != null) {
            cnn.close();
        }
    }

    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        try (InputStream in = PsqlStore.class
                .getClassLoader()
                .getResourceAsStream("jdbc.properties")) {
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        PsqlStore psqlStore = new PsqlStore(properties);
        psqlStore.save(new Post(
                "test_name1", "test_link1", "test_description1", LocalDateTime.now()));
        psqlStore.save(new Post(
                "test_name2", "test_link2", "test_description2", LocalDateTime.now()));
        psqlStore.save(new Post(
                "test_name3", "test_link3", "test_description3", LocalDateTime.now()));
        System.out.println(psqlStore.getAll());
        System.out.println();
        System.out.println(psqlStore.findById(3));
    }
}
