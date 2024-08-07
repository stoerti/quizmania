const HEARTBEAT = 5000

setInterval(fetchServerTime, HEARTBEAT)

const LAST_SERVER_TIME_MODIFIERS: number[] = [];
let CURRENT_SERVER_TIME_MODIFIER: number = 0;

type ServerTimeDto = {
  serverTime: string,
  timeDiff: number
}

function fetchServerTime() {
  const preFetchTime = new Date().getTime()

  fetch('/api/time/sync?clientTime=' + new Date().toISOString(), {
    method: 'GET',
  })
    .then((response) => response.json() as Promise<ServerTimeDto>)
    .then((serverTimeDto) => {
      const serverTime = Date.parse(serverTimeDto.serverTime)
      const postFetchTime = new Date().getTime()
      const timeModifier = postFetchTime - serverTime - (postFetchTime - preFetchTime - serverTimeDto.timeDiff)

      if (LAST_SERVER_TIME_MODIFIERS.length == 5) {
        LAST_SERVER_TIME_MODIFIERS.shift()
      }
      LAST_SERVER_TIME_MODIFIERS.push(timeModifier)
      CURRENT_SERVER_TIME_MODIFIER = (LAST_SERVER_TIME_MODIFIERS.reduce((a, b) => a + b, 0) / LAST_SERVER_TIME_MODIFIERS.length) || 0;

      console.log("Current server time: " + currentServerTime().toISOString())
    })
}

export const currentServerTime = (): Date => {
  const currentMillis = new Date().getTime()
  return new Date(currentMillis + CURRENT_SERVER_TIME_MODIFIER)
}
