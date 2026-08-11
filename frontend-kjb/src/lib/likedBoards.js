// The backend's GET /api/board/{id} doesn't report whether the current user
// has liked the post, so we can't know the initial heart state from the API.
// This tracks "liked by me" locally (scoped per account) so the heart icon
// stays accurate across visits instead of always resetting to unliked.
const PREFIX = "likedBoards:";

function readIds(email) {
  if (!email) return [];
  try {
    const raw = localStorage.getItem(PREFIX + email);
    const ids = raw ? JSON.parse(raw) : [];
    return Array.isArray(ids) ? ids : [];
  } catch {
    return [];
  }
}

export function isBoardLiked(email, boardId) {
  return readIds(email).includes(boardId);
}

export function setBoardLiked(email, boardId, liked) {
  if (!email) return;
  const ids = new Set(readIds(email));
  if (liked) ids.add(boardId);
  else ids.delete(boardId);
  try {
    localStorage.setItem(PREFIX + email, JSON.stringify([...ids]));
  } catch {
    // ignore storage errors (e.g. private browsing quota)
  }
}
