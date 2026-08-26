import Link from "next/link";
import { CategoryBadge } from "@/components/CategoryBadge";
import { HeartIcon } from "@/components/icons";
import { formatRelativeTime } from "@/utils/format";

export function PostList({ posts, emptyText }) {
  if (posts.length === 0) {
    return <p className="py-10 text-center text-sm text-ink-muted">{emptyText}</p>;
  }
  return (
    <ul className="flex flex-col gap-2.5">
      {posts.map((post) => (
        <li key={post.id}>
          <Link
            href={`/board/${post.id}`}
            className="flex flex-col gap-1.5 rounded-xl border border-border bg-white p-3.5 active:scale-[0.99]"
          >
            <div className="flex items-center justify-between">
              <CategoryBadge category={post.category} />
              <span className="text-xs text-ink-muted">{formatRelativeTime(post.createdAt)}</span>
            </div>
            <p className="truncate text-sm font-bold">{post.title}</p>
            <div className="flex items-center gap-3 text-xs text-ink-muted">
              <span>{post.writer}</span>
              <span className="flex items-center gap-1">
                <HeartIcon size={13} /> {post.likeCount}
              </span>
            </div>
          </Link>
        </li>
      ))}
    </ul>
  );
}
