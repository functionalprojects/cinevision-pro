package com.kaankaplan.movieService.business.concretes;

import com.kaankaplan.movieService.business.abstracts.MovieImageService;
import com.kaankaplan.movieService.business.abstracts.MovieService;
import com.kaankaplan.movieService.client.UserAuthorizationClient;
import com.kaankaplan.movieService.dao.MovieImageDao;
import com.kaankaplan.movieService.entity.Movie;
import com.kaankaplan.movieService.entity.MovieImage;
import com.kaankaplan.movieService.entity.dto.ImageRequestDto;
import com.kaankaplan.movieService.storage.PosterStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class MovieImageServiceImpl implements MovieImageService {

    private final MovieImageDao movieImageDao;
    private final MovieService movieService;
    private final UserAuthorizationClient userAuthorizationClient;
    private final PosterStorageService posterStorageService;


    @Override
    public MovieImage addMovieImage(ImageRequestDto imageRequestDto) {

        if (userAuthorizationClient.isAdmin(imageRequestDto.getToken())) {
            Movie movie = movieService.getMovieById(imageRequestDto.getMovieId());

            MovieImage image = movieImageDao.findByMovieMovieId(movie.getMovieId())
                    .orElseGet(MovieImage::new);

            String storedImageUrl = posterStorageService.storePoster(imageRequestDto.getImageUrl(), movie.getMovieId());

            image.setImageUrl(storedImageUrl);
            image.setMovie(movie);

            return movieImageDao.save(image);
        }
        throw new RuntimeException("User is not authorized to update movie artwork.");
    }
}

