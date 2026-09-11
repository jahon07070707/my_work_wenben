import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

let client = null
const subscriptions = []

export function connectWebSocket(topic, callback) {
  if (client?.connected) {
    subscribe(topic, callback)
    return
  }

  client = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    reconnectDelay: 5000,
    onConnect: () => {
      subscribe(topic, callback)
    },
    onStompError: (frame) => {
      console.warn('WebSocket error:', frame.headers?.message)
    }
  })
  client.activate()
}

function subscribe(topic, callback) {
  if (!client?.connected) return
  const sub = client.subscribe(topic, (msg) => {
    try {
      callback(JSON.parse(msg.body))
    } catch (e) {
      callback(msg.body)
    }
  })
  subscriptions.push(sub)
}

export function disconnectWebSocket() {
  subscriptions.forEach(s => s.unsubscribe())
  subscriptions.length = 0
  if (client) {
    client.deactivate()
    client = null
  }
}
