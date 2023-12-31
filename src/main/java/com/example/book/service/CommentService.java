package com.example.book.service;

import com.example.book.domain.CommentDetails;
import com.example.book.domain.UserInfoDetails;
import com.example.book.entity.Comment;
import com.example.book.entity.Post;
import com.example.book.entity.User;
import com.example.book.repository.CommentRepository;
import com.example.book.repository.PostRepository;
import com.example.book.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ModelMapper mapper;

    public Comment writeCommentForPost(UUID post_id, String comment_text){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findUserByEmail(auth.getName()).get();
        Optional<Post> post = postRepository.findById(post_id);
        Comment comment = new Comment(user,post.get());
        comment.setText(comment_text);
        comment.setCreated_time(LocalDateTime.now());
        return commentRepository.save(comment);
    }

    public Comment updateCommentForPost(Integer comment_id, String comment_text){
        Comment comment = commentRepository.findById(comment_id).orElseThrow();
        comment.setText(comment_text);
        comment.setCreated_time(LocalDateTime.now());
        return commentRepository.save(comment);
    }

    public Comment updateReplyForComment(Integer reply_id, String reply_text){
        Optional<Comment> existing_reply = commentRepository.findCommentByID(reply_id);
        if (existing_reply.isPresent()){
            Comment reply = existing_reply.get();
            System.out.println(reply.getId());
            reply.setText(reply_text);
            reply.setCreated_time(LocalDateTime.now());
            return commentRepository.save(reply);
        }
        else {
            System.out.println("Reply not found");
            return null;
        }
    }

    public void deleteComment(Integer comment_id){
        Comment comment = commentRepository.findById(comment_id).orElseThrow();
        commentRepository.delete(comment);
    }

    public void deleteReply(Integer reply_id){
        Optional<Comment> reply = commentRepository.findCommentByID(reply_id);
        if (reply.isPresent()){
            commentRepository.delete(reply.get());
        }
        else {
            System.out.println("Reply not found");
        }
    }

    public Comment saveReply(Integer comment_id, String reply){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findUserByEmail(auth.getName()).get();
        Optional<Comment> comment = commentRepository.findCommentByID(comment_id);
        if (comment.isPresent()){
            Comment reply_comment = new Comment();
            reply_comment.setUser(user);
            reply_comment.setParent(comment.get());
            reply_comment.setText(reply);
            reply_comment.setCreated_time(LocalDateTime.now());
            return commentRepository.save(reply_comment);
        }
        else {
            System.out.println("The current comment you are reply to are not exist.");
            return null;
        }
    }

    // Get top-level comments for a post
    public List<CommentDetails> viewCommentsInPost(UUID post_id){
        List<Comment> comments = commentRepository.getCommentsForPost(post_id);
        List<CommentDetails> commentDTOList = new ArrayList<>();
        for (Comment comment : comments){
            CommentDetails commentDetails = configureCommentCreatorImage(comment);
            commentDetails.setReplies(viewRepliesInComment(comment));
            commentDetails.setUpdated_time(calculateCommentCreatedTimeGap(comment.getCreated_time()));
            commentDTOList.add(commentDetails);
        }
        return commentDTOList;
    }

    public List<CommentDetails> viewRepliesInComment(Comment comment){
        List<Comment> replies = commentRepository.getRepliesByComment(comment);
        List<CommentDetails> replyDetailList = new ArrayList<>();
        for (Comment reply : replies){
            CommentDetails replyDetails = configureCommentCreatorImage(reply);
            replyDetails.setUpdated_time(calculateCommentCreatedTimeGap(reply.getCreated_time()));
            replyDetailList.add(replyDetails);
        }
        return replyDetailList;
    }

    private CommentDetails configureCommentCreatorImage(Comment comment){
        User creator = comment.getUser();
        CommentDetails commentDetails = mapper.map(comment, CommentDetails.class);
        // Mapper for comment creator image
        UserInfoDetails creator_detail = mapper.map(creator, UserInfoDetails.class);
        // Set creator image for post
        commentDetails.setCreator(creator_detail);
        return commentDetails;
    }

    private String calculateCommentCreatedTimeGap(LocalDateTime created_time){
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(created_time, now);
        long seconds = duration.getSeconds();

        if (seconds >= 31536000) {
            long years = seconds / 31536000;
            return years + " year(s) ago";
        } else if (seconds >= 2592000) {
            long months = seconds / 2592000;
            return months + " month(s) ago";
        } else if (seconds >= 86400) {
            long days = seconds / 86400;
            return days + " day(s) ago";
        } else if (seconds >= 3600) {
            long hours = seconds / 3600;
            return hours + " hour(s) ago";
        } else if (seconds >= 60) {
            long minutes = seconds / 60;
            return minutes + " minute(s) ago";
        } else {
            return seconds + " second(s) ago";
        }
    }
}
