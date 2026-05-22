package com.kaankaplan.movieService.business.concretes;

import com.kaankaplan.movieService.business.abstracts.DirectorService;
import com.kaankaplan.movieService.client.UserAuthorizationClient;
import com.kaankaplan.movieService.dao.DirectorDao;
import com.kaankaplan.movieService.entity.Director;
import com.kaankaplan.movieService.entity.dto.DirectorRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectorServiceImpl implements DirectorService {

    private final DirectorDao directorDao;
    private final UserAuthorizationClient userAuthorizationClient;

    @Cacheable(value = "directors")
    @Override
    public List<Director> getall() {
        return directorDao.findAll(Sort.by(Sort.Direction.ASC, "directorName"));
    }

    @Override
    public Director getDirectorById(int directorId) {
        return directorDao.getDirectorByDirectorId(directorId);
    }

    @CacheEvict(value = "directors", allEntries = true)
    @Override
    public Director add(DirectorRequestDto directorRequestDto)
    {
        if (userAuthorizationClient.isAdmin(directorRequestDto.getToken())) {
            Director director = Director.builder()
                    .directorName(directorRequestDto.getDirectorName())
                    .build();
            return directorDao.save(director);
        }
        throw new RuntimeException("User is not authorized to add a director.");
    }
}

