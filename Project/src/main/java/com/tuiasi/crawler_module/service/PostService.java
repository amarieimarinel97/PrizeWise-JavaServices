package com.tuiasi.crawler_module.service;

import com.tuiasi.crawler_module.model.Post;
import com.tuiasi.crawler_module.model.SocialNetwork;
import com.tuiasi.crawler_module.repository.ICrudRepository;
import com.tuiasi.crawler_module.repository.PostRepository;
import com.tuiasi.exception.DatabaseConnectionException;
import com.tuiasi.exception.ObjectNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class PostService implements ICrudService<Post, Integer> {

    private PostRepository repository;


    @Autowired
    public PostService(ICrudRepository<Post, Integer> postRepository) {
        this.repository = (PostRepository) postRepository;
    }


    @Override
    public Post add(Post post) {
        try {
            return repository.add(post);
        } catch (Exception e) {
            log.error("Could not add post with id: " + post.getId() + " : " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    @Override
    public Post get(Integer id) {
        try {
            Optional<Post> post = repository.get(id);
            return post
                    .orElseThrow(() -> new ObjectNotFoundException("Post with id: " + id + " does not exist"));
        } catch (Exception e) {
            log.error("Could not get post with id: " + id + " : " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    @Override
    public Post update(Post post, Integer id) {
        try {
            Optional<Post> result = repository.update(post, id);
            return result
                    .orElseThrow(() -> new ObjectNotFoundException("Post with id: " + post.getId() + " does not exist"));
        } catch (Exception e) {
            log.error("Could not update post with id: " + id + " : " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    @Override
    public void delete(Integer id) {

        try {
            repository.delete(id);
        } catch (ObjectNotFoundException e) {
            log.error(e.getMessage());
        } catch (Exception e) {
            log.error("Could not delete article with id: " + id + " : " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    public List<Post> getLastPostsBySymbol(String symbol, int numberOfPosts) {
        try {
            return repository.getLastPostsBySymbol(symbol, numberOfPosts);
        } catch (Exception e) {
            log.error("Could not retrieve posts: " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    public List<Post> getRecentPosts(int numberOfPosts, SocialNetwork type) {
        try {
            return repository.getRecentPosts(numberOfPosts, type);
        } catch (Exception e) {
            log.error("Could not retrieve posts: " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }
}
