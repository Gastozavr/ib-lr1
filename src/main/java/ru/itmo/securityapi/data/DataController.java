package ru.itmo.securityapi.data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequestMapping("/api/data")
public class DataController {

    private final PostRepository postRepository;

    public DataController(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @GetMapping
    public List<PostResponse> getData(
            @RequestParam(name = "q", defaultValue = "") @Size(max = 120) String query) {
        List<Post> posts = query.isBlank()
                ? postRepository.findAll()
                : postRepository.findByTitleContainingIgnoreCaseOrderByIdAsc(query);
        return posts.stream().map(PostResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createData(@Valid @RequestBody CreatePostRequest request, Principal principal) {
        Post post = new Post(
                HtmlUtils.htmlEscape(request.title()),
                HtmlUtils.htmlEscape(request.content()),
                principal.getName());
        return PostResponse.from(postRepository.save(post));
    }

    public record CreatePostRequest(
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 1000) String content) {
    }

    public record PostResponse(Long id, String title, String content, String author) {
        static PostResponse from(Post post) {
            return new PostResponse(post.getId(), post.getTitle(), post.getContent(), post.getAuthor());
        }
    }
}
