export default function ErrorBanner({ message }) {
  if (!message) return null
  return <p className="error-text">{message}</p>
}
