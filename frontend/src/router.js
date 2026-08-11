import { useCallback, useEffect, useState } from 'react'

function getHashPath() {
  const hash = window.location.hash.slice(1)
  return hash || '/'
}

export function useHashLocation() {
  const [path, setPath] = useState(getHashPath())

  useEffect(() => {
    const onChange = () => setPath(getHashPath())
    window.addEventListener('hashchange', onChange)
    return () => window.removeEventListener('hashchange', onChange)
  }, [])

  const navigate = useCallback((to) => {
    if (getHashPath() === to) {
      // 같은 경로로 이동 시에도 강제로 갱신 트리거
      window.dispatchEvent(new Event('hashchange'))
    }
    window.location.hash = to
  }, [])

  return [path, navigate]
}

// '/board/:id' 같은 패턴을 실제 경로와 비교해 params를 뽑아낸다.
export function matchPath(pattern, path) {
  const patternParts = pattern.split('/').filter(Boolean)
  const pathParts = path.split('/').filter(Boolean)
  if (patternParts.length !== pathParts.length) return null

  const params = {}
  for (let i = 0; i < patternParts.length; i++) {
    const pp = patternParts[i]
    const ap = pathParts[i]
    if (pp.startsWith(':')) {
      params[pp.slice(1)] = decodeURIComponent(ap)
    } else if (pp !== ap) {
      return null
    }
  }
  return params
}
