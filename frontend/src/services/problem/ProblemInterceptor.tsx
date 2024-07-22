
export type ProblemJson = {
  type: string,
  title: string | undefined,
  detail: string | undefined,
  context: { [key: string]: unknown },
}

const KNOWN_PROBLEMS = new Map<string, (problem: ProblemJson) => Error>()

const {fetch: originalFetch} = window;
window.fetch = async (...args) => {
  const [resource, config] = args;
  const response = await originalFetch(resource, config);

  if (!response.ok && response.headers.get('Content-Type') === 'application/problem+json') {
    const problem = await response.json() as ProblemJson
    console.log("Detected problem+json of type " + problem.type)
    throw resolveProblemException(problem)
  }

  return response;
};

const resolveProblemException = (problem: ProblemJson): Error => {
  if (KNOWN_PROBLEMS.has(problem.type)) {
    return KNOWN_PROBLEMS.get(problem.type)!(problem)
  }

  return Error(problem.title)
}

export const registerProblemFactory = (type: string, factory: (problem: ProblemJson) => Error) => {
  KNOWN_PROBLEMS.set(type, factory)
}
