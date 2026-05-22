package com.kaankaplan.movieService.business.concretes;

import com.kaankaplan.movieService.business.abstracts.CategoryService;
import com.kaankaplan.movieService.business.abstracts.DirectorService;
import com.kaankaplan.movieService.business.abstracts.MovieService;
import com.kaankaplan.movieService.client.UserAuthorizationClient;
import com.kaankaplan.movieService.dao.MovieDao;
import com.kaankaplan.movieService.entity.Category;
import com.kaankaplan.movieService.entity.Director;
import com.kaankaplan.movieService.entity.Movie;
import com.kaankaplan.movieService.entity.dto.MovieRequestDto;
import com.kaankaplan.movieService.entity.dto.MovieResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private final MovieDao movieDao;
    private final CategoryService categoryService;
    private final DirectorService directorService;
    private final UserAuthorizationClient userAuthorizationClient;

    @Override
    @Cacheable("displaying_movies")
    public List<MovieResponseDto> getAllDisplayingMoviesInVision() {
        return movieDao.getAllDisplayingMoviesInVision();
    }

    @Override
    @Cacheable("comingSoon_movies")
    public List<MovieResponseDto> getAllComingSoonMovies() {
        return movieDao.getAllComingSoonMovies();
    }

    @Override
    public MovieResponseDto getMovieByMovieId(int movieId) {
        return movieDao.getMovieById(movieId);
    }

    @Override
    public Movie getMovieById(int movieId) {
        return movieDao.getMovieByMovieId(movieId);
    }

    @Override
    @CacheEvict(value = {"displaying_movies", "comingSoon_movies"}, allEntries = true)
    public Movie addMovie(MovieRequestDto movieRequestDto) {
        if (!userAuthorizationClient.isAdmin(movieRequestDto.getUserAccessToken())) {
            throw new RuntimeException("User is not authorized to add a movie.");
        }

        Category category = categoryService.getCategoryById(movieRequestDto.getCategoryId());
        Director director = directorService.getDirectorById(movieRequestDto.getDirectorId());

        Movie movie = Movie.builder()
                .movieName(movieRequestDto.getMovieName())
                .description(movieRequestDto.getDescription())
                .duration(movieRequestDto.getDuration())
                .releaseDate(movieRequestDto.getReleaseDate())
                .movieTrailerUrl(movieRequestDto.getTrailerUrl())
                .category(category)
                .director(director)
                .isDisplay(movieRequestDto.isInVision())
                .build();
        return movieDao.save(movie);
    }
}
