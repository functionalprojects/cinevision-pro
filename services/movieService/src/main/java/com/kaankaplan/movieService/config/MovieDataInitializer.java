package com.kaankaplan.movieService.config;

import com.kaankaplan.movieService.dao.ActorDao;
import com.kaankaplan.movieService.dao.CategoryDao;
import com.kaankaplan.movieService.dao.CityDao;
import com.kaankaplan.movieService.dao.DirectorDao;
import com.kaankaplan.movieService.dao.MovieDao;
import com.kaankaplan.movieService.dao.MovieImageDao;
import com.kaankaplan.movieService.dao.MovieSaloonTimeDao;
import com.kaankaplan.movieService.dao.SaloonDao;
import com.kaankaplan.movieService.entity.Actor;
import com.kaankaplan.movieService.entity.Category;
import com.kaankaplan.movieService.entity.City;
import com.kaankaplan.movieService.entity.Director;
import com.kaankaplan.movieService.entity.Movie;
import com.kaankaplan.movieService.entity.MovieImage;
import com.kaankaplan.movieService.entity.MovieSaloonTime;
import com.kaankaplan.movieService.entity.Saloon;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Date;
import java.time.LocalDate;

@Configuration
@RequiredArgsConstructor
public class MovieDataInitializer {

    private final MovieDao movieDao;
    private final CategoryDao categoryDao;
    private final DirectorDao directorDao;
    private final MovieImageDao movieImageDao;
    private final ActorDao actorDao;
    private final CityDao cityDao;
    private final SaloonDao saloonDao;
    private final MovieSaloonTimeDao movieSaloonTimeDao;

    @Bean
    public CommandLineRunner seedMovieCatalog() {
        return args -> {
            if (movieDao.count() > 0) {
                return;
            }

            Category sciFi = categoryDao.save(new Category(0, "Sci-Fi", null));
            Category action = categoryDao.save(new Category(0, "Action", null));

            Director nolan = directorDao.save(Director.builder().directorName("Christopher Nolan").build());
            Director villeneuve = directorDao.save(Director.builder().directorName("Denis Villeneuve").build());

            Movie interstellar = movieDao.save(Movie.builder()
                    .movieName("Interstellar")
                    .description("A team of explorers travels through a wormhole in space to secure humanity's future.")
                    .duration(169)
                    .releaseDate(Date.valueOf(LocalDate.of(2014, 11, 7)))
                    .isDisplay(true)
                    .movieTrailerUrl("https://www.youtube.com/embed/zSWdZVtXT7E")
                    .category(sciFi)
                    .director(nolan)
                    .build());

            Movie duneMessiah = movieDao.save(Movie.builder()
                    .movieName("Dune: Messiah")
                    .description("Paul Atreides faces the political and personal cost of prophecy in the next chapter of Arrakis.")
                    .duration(155)
                    .releaseDate(Date.valueOf(LocalDate.now().plusMonths(3)))
                    .isDisplay(false)
                    .movieTrailerUrl("https://www.youtube.com/embed/Way9Dexny3w")
                    .category(action)
                    .director(villeneuve)
                    .build());

            movieImageDao.save(new MovieImage(0, "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=900&q=80", interstellar));
            movieImageDao.save(new MovieImage(0, "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=900&q=80", duneMessiah));

            actorDao.save(Actor.builder().actorName("Matthew McConaughey").movie(interstellar).build());
            actorDao.save(Actor.builder().actorName("Anne Hathaway").movie(interstellar).build());
            actorDao.save(Actor.builder().actorName("Timothee Chalamet").movie(duneMessiah).build());
            actorDao.save(Actor.builder().actorName("Zendaya").movie(duneMessiah).build());

            City douala = cityDao.save(City.builder().cityName("Douala").movie(interstellar).build());
            City yaounde = cityDao.save(City.builder().cityName("Yaounde").movie(interstellar).build());

            Saloon doualaHall1 = saloonDao.save(new Saloon(0, "Bonanjo Hall 1", douala));
            Saloon doualaHall2 = saloonDao.save(new Saloon(0, "Akwa Hall 3", douala));
            Saloon yaoundeHall = saloonDao.save(new Saloon(0, "Centre Hall 2", yaounde));

            movieSaloonTimeDao.save(new MovieSaloonTime(0, "18:00", doualaHall1, interstellar));
            movieSaloonTimeDao.save(new MovieSaloonTime(0, "20:45", doualaHall2, interstellar));
            movieSaloonTimeDao.save(new MovieSaloonTime(0, "21:15", yaoundeHall, interstellar));
        };
    }
}
