package com.datalayer;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import com.data.Comment;
import com.data.ExternalUserQuery;
import com.data.FetchLikeCommentQuery;
import com.data.Followers;
import com.data.Likes;
import com.data.Post;
import com.data.PostMessage;
import com.data.Story;
import com.data.Userdetails;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class RDBDaoLayer {
	private static final String fetchLastestFollowersStory = "SELECT DISTINCT(accountname) FROM story WHERE stime > NOW() - INTERVAL 24 MONTH AND accountname IN (SELECT DISTINCT(followee) FROM followers WHERE follower = (acc) )";
	private static final Path basePathForProfile = Paths.get("/home/kaustubh/social-file-data/profiles/");
	private static final Path basePathForPost = Paths.get("/home/kaustubh/social-file-data/posts/");
	private static final Path basePathForStories = Paths.get("/home/kaustubh/social-file-data/stories/");
	private static final String passwordChangeQuery = "UPDATE userdetails SET password = :password WHERE accountname = :accountname";

	@Autowired
	R2dbcEntityTemplate r2dbcEntityTemplate;

	public Mono<Userdetails> getUserDetails(String accountname) {
		return r2dbcEntityTemplate.selectOne(Query.query(Criteria.where("accountname").is(accountname)).limit(1),
				Userdetails.class);
	}

	public Mono<Post> getPost(Integer postid) {
		return r2dbcEntityTemplate.selectOne(Query.query(Criteria.where("postid").is(postid)), Post.class);
	}

	public Mono<List<String>> getFollowersAccountNames(String accountName, String lfn) {
		Criteria cr = Criteria.where("followee").is(accountName);

		if (lfn != null) {
			cr = cr.and(Criteria.where("follower").greaterThan(lfn));
		}

		return r2dbcEntityTemplate.select(Query.query(cr).sort(Sort.by("follower").ascending()), Followers.class)
				.map(x -> {
					return x.getFollower();
				}).collectList();
	}

	public Mono<List<String>> getFolloweeAccountNames(String accountName, String lfn) {
		Criteria cr = Criteria.where("follower").is(accountName);

		if (lfn != null) {
			cr = cr.and(Criteria.where("followee").greaterThan(lfn));
		}

		return r2dbcEntityTemplate.select(Query.query(cr).sort(Sort.by("followee").ascending()), Followers.class)
				.map(x -> {
					return x.getFollowee();
				}).collectList();
	}
	
	public Mono<List<Story>> getUserLatestStories(String accountname) {
		String sqlQuery = "SELECT * FROM story WHERE accountname = \""+accountname+"\" AND stime > NOW() - INTERVAL 25 HOUR";
		return r2dbcEntityTemplate.getDatabaseClient().sql(sqlQuery).map(row -> {
			Story story = new Story();
			story.setAccountname(row.get("accountname",String.class));
			story.setImgurl(row.get("imgurl",String.class));
			story.setStoryid((row.get("storyid",Integer.class)));
			story.setIsmemory(((row.get("ismemory",Long.class))));
			story.setStime(row.get("stime",Instant.class));
			return story;
		}).all().collectList();
	}

	public Mono<Story> getStory(Integer storyid) {
		return r2dbcEntityTemplate.selectOne(Query.query(Criteria.where("storyid").is(storyid)), Story.class);
	}

	public Mono<Userdetails> signup(String accountname, String password, String url, String username, String fkey) {

		Userdetails userdetails = new Userdetails();
		userdetails.setAccountname(accountname);
		userdetails.setPassword(password);
		userdetails.setProfileurl(url);
		userdetails.setUsername(username);
		userdetails.setFkey(fkey);

		return r2dbcEntityTemplate.insert(userdetails);
	}
	
	public Mono<List<String>> getFollowersForStories(String accountname) {
		String sqlQuery = "SELECT DISTINCT(accountname) FROM story WHERE stime > NOW() - INTERVAL 24 HOUR AND accountname IN (SELECT DISTINCT(followee) FROM followers WHERE follower = \""+accountname+"\")";
		return r2dbcEntityTemplate.getDatabaseClient().sql(sqlQuery).map(row -> {
			return row.get(0, String.class);
		}).all().collectList();
	}

	public Mono<Long> changePassword(String accountName, String newPassword) {
		return r2dbcEntityTemplate.update(Query.query(Criteria.where("accountname").is(accountName)),
				Update.update("password", newPassword), Userdetails.class);
	}

	public Mono<Long> updateProfileUrl(String accountName, String fileUrl) {
		return r2dbcEntityTemplate.update(Query.query(Criteria.where("accountname").is(accountName)),
				Update.update("profileurl", fileUrl), Userdetails.class);
	}

	public Mono<Long> updateFkey(String accountName, String fkey) {
		return r2dbcEntityTemplate.update(Query.query(Criteria.where("accountname").is(accountName)),
				Update.update("fkey", fkey), Userdetails.class);
	}

	public Mono<Long> invalidateFkey(String accountName, String invalidKey) {
		return r2dbcEntityTemplate.update(
				Query.query(Criteria.where("accountname").is(accountName).and(Criteria.where("fkey").is(invalidKey))),
				Update.update("fkey", ""), Userdetails.class);
	}

	public Mono<Userdetails> signup(Userdetails userdetails) {

		return r2dbcEntityTemplate.insert(userdetails);
	}

	public Mono<Long> updateUserDetail(Userdetails userdetails) {
		return r2dbcEntityTemplate.update(Query.query(Criteria.where("accountname").is(userdetails.getAccountname())),
				Update.update("delegation", userdetails.getDelegation()).set("about", userdetails.getAbout())
						.set("link", userdetails.getLink()),
				Userdetails.class);
	}

	public Mono<Post> createPost(Post post) {

		return r2dbcEntityTemplate.insert(post);

	}

	public Mono<Long> deletePost(Integer postid) {
		return r2dbcEntityTemplate.delete(Query.query(Criteria.where("postid").is(postid)), Post.class);
	}

	public Mono<String> uploadPostImage(Mono<DataBuffer> fileData, String filePathName) throws IOException {
		Path filepath = basePathForPost.resolve(filePathName);
		AsynchronousFileChannel channel = AsynchronousFileChannel.open(filepath, StandardOpenOption.CREATE,
				StandardOpenOption.WRITE);
		return DataBufferUtils.write(fileData, channel).then(Mono.just(filePathName));
	}

	public Mono<String> uploadStoryImage(Mono<DataBuffer> fileData, String filePathName) throws IOException {
		Path filepath = basePathForStories.resolve(filePathName);
		AsynchronousFileChannel channel = AsynchronousFileChannel.open(filepath, StandardOpenOption.CREATE,
				StandardOpenOption.WRITE);
		return DataBufferUtils.write(fileData, channel).then().doOnNext((t) -> {
			try {
				channel.close();
			} catch (IOException e) {
			}
		}).then(Mono.just(filePathName));
	}

	public Mono<Comment> addComment(String accountname, Integer id, String text) {

		Comment cmt = new Comment();
		cmt.setAccountname(accountname);
		cmt.setOnpostid(id);
		cmt.setText(text);

		return r2dbcEntityTemplate.insert(cmt);
	}

	public Mono<Comment> addComment(Comment comment) {
		return r2dbcEntityTemplate.insert(comment);
	}

	public Mono<Likes> postLike(String accountname, Integer postID) {
		Likes like = new Likes();
		like.setAccountname(accountname);
		like.setOnpostid(postID);
		return r2dbcEntityTemplate.insert(like);
	}

	public Mono<Likes> postLike(Likes like) {
		return r2dbcEntityTemplate.insert(like);
	}

	public Mono<Boolean> postDislike(Likes like) {
		return r2dbcEntityTemplate
				.getDatabaseClient().sql("DELETE FROM likes WHERE onpostid=" + like.getOnpostid()
						+ " and accountname=\"" + like.getAccountname() + "\"")
				.fetch().first().then(Mono.just(true)).onErrorReturn(false);
	}

	public Mono<Followers> followUser(Followers followers) {
		return r2dbcEntityTemplate.insert(followers);
	}

	public Mono<Boolean> unfollowUser(Followers followers) {
		return r2dbcEntityTemplate.getDatabaseClient().sql("DELETE FROM followers WHERE follower=\""
				+ followers.getFollower() + "\" and followee=\"" + followers.getFollowee() + "\"").then()
				.then(Mono.just(true));
	}

	public Flux<String> getFollowers(String accountname) {
		return r2dbcEntityTemplate.select(Query.query(Criteria.where("followee").is(accountname)), Followers.class)
				.map((follower) -> follower.getFollower());
	}

	public Mono<Boolean> checkIfUserFollowed(ExternalUserQuery euq) {
		return r2dbcEntityTemplate.exists(Query.query(Criteria.where("follower").is(euq.getAccountName())
				.and(Criteria.where("followee").is(euq.getSearchedAccountName()))), Followers.class);
	}

	public Flux<PostMessage> getUserPostsFromDb(String accountName, List<Integer> postids) {

		if (postids == null || postids.isEmpty()) {
			return Flux.empty();
		}
		String sql = "SELECT COUNT(id) totallikes, MIN(postid) postid, MIN(imgurl) imgurl, MIN(ctime) ctime, MIN(caption) caption, MIN(post.accountname) creatorname, MAX(IF(likes.accountname=\""
				+ accountName
				+ "\",1,0)) liked FROM post LEFT JOIN likes ON post.postid=likes.onpostid WHERE post.postid IN (:pid) GROUP BY post.postid";

		return r2dbcEntityTemplate.getDatabaseClient().sql(sql).bind("pid", postids).map((row) -> {
			PostMessage post = new PostMessage();
			post.setAccountname(row.get("creatorname", String.class));
			post.setCaption(row.get("caption", String.class));
			post.setCtime(row.get("ctime", Instant.class));
			post.setImgurl(row.get("imgurl", String.class));
			post.setLikesCount(row.get("totallikes", Long.class));
			post.setPostid((row.get("postid", Long.class)));
			post.setLiked((row.get("liked", Long.class)));

			return post;
		}).all();
	}

	public Mono<PostMessage> getUserPostsFromDb(Integer postid) {
		String query = "SELECT * FROM (SELECT COUNT(id) likesCount FROM likes WHERE onpostid = " + postid
				+ ") T1 JOIN (SELECT * FROM post WHERE postid = " + postid + ") T2";

		return r2dbcEntityTemplate.getDatabaseClient().sql(query).map((row) -> {
			PostMessage post = new PostMessage();
			post.setAccountname(row.get("accountname", String.class));
			post.setCaption(row.get("caption", String.class));
			post.setCtime(row.get("ctime", Instant.class));
			post.setImgurl(row.get("imgurl", String.class));
			post.setLikesCount(row.get("likesCount", Long.class));
			post.setPostid((row.get("postid", Long.class)));
			post.setLiked(0l);

			return post;
		}).first();
	}

	public Mono<Post> getPostActivity(Likes like) {
		return r2dbcEntityTemplate.selectOne(Query.query(Criteria.where("postid").is(like.getOnpostid())), Post.class);
	}

	public Mono<Post> getPostAvtivity(Comment comment) {
		return r2dbcEntityTemplate.selectOne(Query.query(Criteria.where("postid").is(comment.getOnpostid())),
				Post.class);
	}

	public Mono<Story> postStory(Story story) {
		story.setStime(Instant.now());
		return r2dbcEntityTemplate.insert(story);
	}

	public Mono<List<Story>> getStoriesOf(List<Integer> storyids) {
		return r2dbcEntityTemplate.select(Query.query(Criteria.where("storyid").in(storyids)), Story.class)
				.collectList();
	}

	public Mono<List<Post>> getUserPersonalPostsList(String accountName) {
		return r2dbcEntityTemplate.select(Query.query(Criteria.where("accountname").is(accountName)), Post.class)
				.collectList();
	}

	public Flux<Story> getStoriesFlux(List<Integer> storyids) {
		return r2dbcEntityTemplate.select(Query.query(Criteria.where("storyid").in(storyids)), Story.class);
	}

	public Mono<Integer> uploadProfileImage(Mono<FilePart> imagefile, String fileName) {
		return imagefile.flatMap((img) -> {
			Path filepath = basePathForProfile.resolve(fileName);
			return img.transferTo(filepath).then(Mono.just(200));
		}).onErrorReturn(500);
	}

	public Mono<Integer> uploadProfileImage2(FilePart imagefile, String fileName) {

		Path filepath = basePathForProfile.resolve(fileName);
		return imagefile.transferTo(filepath).then(Mono.just(200));

	}

	public Mono<List<Story>> getUserMemoriesList(String accountName) {
		Instant now = Instant.now();
		long difference = now.getEpochSecond() - 86400l;

		return r2dbcEntityTemplate.getDatabaseClient()
				.sql("SELECT MIN(imgurl) minimgurl,ismemory FROM story WHERE ismemory IS NOT NULL AND ismemory<"
						+ difference + " AND accountname=\"" + accountName + "\" GROUP BY ismemory")
				.map(row -> {
					Story story = new Story();
					story.setAccountname(accountName);
					story.setImgurl((String) row.get("minimgurl", String.class));
					story.setIsmemory((Long) (row.get("ismemory", Long.class)));

					return story;
				}).all().collectList();
	}

	public Mono<List<Story>> getStoryFromMemoryId(Long memoryId) {
		return r2dbcEntityTemplate
				.select(Query.query(Criteria.where("ismemory").is(memoryId)).sort(Sort.by("storyid").ascending()),
						Story.class)
				.collectList();
	}

	public Mono<List<Comment>> fetchComments(FetchLikeCommentQuery fcq) {
		Query query;
		if (fcq.getLastFetchId() == null) {
			query = Query.query(Criteria.where("onpostid").is(fcq.getPostid())).limit(50);
		} else {
			query = Query.query(Criteria.where("onpostid").is(fcq.getPostid())
					.and(Criteria.where("commentid").lessThan(fcq.getLastFetchId()))).limit(50);
		}
		return r2dbcEntityTemplate.select(query, Comment.class).collectList();
	}

	public Mono<List<Likes>> fetchLikes(FetchLikeCommentQuery fcq) {
		Query query;
		if (fcq.getLastFetchId() == null) {
			query = Query.query(Criteria.where("onpostid").is(fcq.getPostid())).limit(50);
		} else {
			query = Query.query(Criteria.where("onpostid").is(fcq.getPostid())
					.and(Criteria.where("id").lessThan(fcq.getLastFetchId()))).limit(50);
		}
		return r2dbcEntityTemplate.select(query, Likes.class).collectList();
	}

}
