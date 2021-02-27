package com.company;

import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;


import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws Exception {

        // 1. check user input
        var directory = args.length != 1 ? "..\\..\\..\\mp3 Archive Project\\test mp3s" : args[0];

        var mp3Directory = Paths.get(directory);

        if (!Files.exists(mp3Directory))
            throw new IllegalArgumentException("The specified directory does not exist : " + mp3Directory);

        // 2. Files
        var mp3Paths = new ArrayList<Path>();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(mp3Directory, "*.mp3")) {
            paths.forEach(p -> {
               System.out.println(p);
               mp3Paths.add(p);
            });
        }

        // 3. Files -> domain classes
        List<Song> songs = mp3Paths.stream().map(p -> {
           try {
               var mp3File = new Mp3File(p);
               var id3 = mp3File.getId3v2Tag();
               return new Song(id3.getArtist(), id3.getYear(), id3.getAlbum(), id3.getTitle());
           } catch (UnsupportedTagException | IOException | InvalidDataException e) {
               e.printStackTrace();
               throw new IllegalStateException(e);
           }
        }).collect(Collectors.toList());

        // 4. Database
        try (var conn = DriverManager.getConnection("jdbc:h2:~/mydatabase;" +
                "AUTO_SERVER=TRUE;INIT=runscript from './create.sql'")) {

            var st = conn.prepareStatement(
                    "insert into SONGS (artist, year, album, title) values (?, ?, ?, ?);");

            for (var song : songs) {
                st.setString(1, song.getArtist());
                st.setString(2, song.getYear());
                st.setString(3, song.getAlbum());
                st.setString(4, song.getTitle());
                st.addBatch();
            }

            var updates = st.executeBatch();
            System.out.println("Inserted [=" + updates.length + "] records into the database");
        }

        // 5. Server
        Server server = new Server(8080);
        ServletContextHandler context = new ServletContextHandler(
                ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.setResourceBase(System.getProperty("java.io.tmpdir"));
        server.setHandler(context);

        context.addServlet(SongServlet.class, "/songs");
        server.start();

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(new URI("http://localhost:8080/songs"));
        }
    }
}
