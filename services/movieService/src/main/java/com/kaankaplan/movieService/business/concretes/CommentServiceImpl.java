package com.kaankaplan.movieService.business.concretes;

import com.kaankaplan.movieService.business.abstracts.CommentService;
import com.kaankaplan.movieService.business.abstracts.MovieService;
import com.kaankaplan.movieService.client.UserAuthorizationClient;
import com.kaankaplan.movieService.dao.CommentDao;
import com.kaankaplan.movieService.entity.Comment;
import com.kaankaplan.movieService.entity.Movie;
import com.kaankaplan.movieService.entity.dto.CommentRequestDto;
import com.kaankaplan.movieService.entity.dto.DeleteCommentRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentDao commentDao;
    private final MovieService movieService;
    private final UserAuthorizationClient userAuthorizationClient;

    @Override
    public List<Comment> getCommentsByMovieId(int movieId, int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo-1, pageSize);
        return commentDao.getCommentsByMovieMovieId(movieId, pageable);
    }

    @Override
    public int getNumberOfCommentsByMovieId(int movieId) {
        return commentDao.countCommentByMovieMovieId(movieId);
    }

    @Override
    public void deleteComment(DeleteCommentRequestDto deleteCommentRequestDto) {

        if (userAuthorizationClient.isCustomer(deleteCommentRequestDto.getToken())) {
            commentDao.deleteById(deleteCommentRequestDto.getCommentId());
            return;
        }

        throw new RuntimeException("User is not authorized to delete this comment.");
    }

    @Override
    public Comment addComment(CommentRequestDto commentRequestDto) {

        if (userAuthorizationClient.isCustomer(commentRequestDto.getToken())) {
            Movie movie = movieService.getMovieById(commentRequestDto.getMovieId());

            Comment comment = Comment.builder()
                    .commentByUserId(commentRequestDto.getCommentByUserId())
                    .commentBy(commentRequestDto.getCommentBy())
                    .commentText(commentRequestDto.getCommentText())
                    .movie(movie)
                    .build();

            return commentDao.save(comment);
        }
        throw new RuntimeException("User is not authorized to add a comment.");
    }
}

