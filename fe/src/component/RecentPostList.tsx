import { type PostSummary, readerSlug } from '../model/Post.ts'
import { contentPath, LIST_PATH, WRITE_PATH } from '../model/route.ts'
import './RecentPostList.css'

interface RecentPostListProps {
  posts: PostSummary[]
}

/** The info area's list: only the titles, each linking to its body, then links to the list of all posts and to the writing area. */
export function RecentPostList({ posts }: RecentPostListProps) {
  return (
    <>
      <h2 className="recent-post-list-heading">최근 글</h2>
      <ul className="recent-post-list">
        {posts.map((post) => (
          <li key={post.id}>
            <a
              className="recent-post-list-link"
              href={contentPath('post', readerSlug(post.slug))}
              title={post.title}
            >
              {post.title}
            </a>
          </li>
        ))}
        <li>
          <a className="recent-post-list-link recent-post-list-all" href={LIST_PATH}>전체 목록</a>
        </li>
        <li>
          <a className="recent-post-list-link recent-post-list-all" href={WRITE_PATH}>
            <span aria-hidden="true">➕</span> 새로운 생각
          </a>
        </li>
      </ul>
    </>
  )
}
