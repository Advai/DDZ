import axios from 'axios';
import type {
  LoginRequest,
  LoginResponse,
  CreateGameRequest,
  CreateGameResponse,
  JoinGameRequest,
  GameInfo,
} from './types';

/**
 * API service for communicating with the DDZ backend.
 * All requests are proxied through Vite to http://localhost:8080
 */

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * Login or create user
 * @param username Username (lowercase, alphanumeric)
 * @param displayName Display name shown to other players
 * @returns User ID, username, and display name
 */
export async function login(
  username: string,
  displayName: string
): Promise<LoginResponse> {
  const request: LoginRequest = {
    username,
    displayName,
  };

  const response = await api.post<LoginResponse>('/auth/login', request);
  return response.data;
}

/**
 * Create a new game
 * @param playerCount Number of players for the game (3-12)
 * @param creatorName Creator's display name
 * @param userId Creator's user ID
 * @returns Game info including game ID and player ID
 */
export async function createGame(
  playerCount: number
): Promise<CreateGameResponse> {
  const request: CreateGameRequest = {
    playerCount,
  };

  const response = await api.post<CreateGameResponse>('/games', request);
  return response.data;
}

/**
 * Join an existing game
 * @param sessionId The session ID to join
 * @param playerName Player's display name
 * @param userId Player's user ID
 * @param seatPosition Optional seat position (0-6)
 * @returns Game info with player ID
 */
export async function joinGame(
  sessionId: string,
  playerName: string,
  userId: string,
  seatPosition?: number,
  creatorToken?: string
): Promise<GameInfo> {
  const request: JoinGameRequest = {
    playerName,
    userId,
    seatPosition,
    creatorToken,
  };

  const response = await api.post<GameInfo>(
    `/games/${sessionId}/join`,
    request
  );
  return response.data;
}

/**
 * Get game state
 * @param sessionId The session ID
 * @param playerId The player's ID
 * @returns Current game state
 */
export async function getGameState(
  sessionId: string,
  playerId: string
): Promise<GameInfo> {
  const response = await api.get<GameInfo>(
    `/games/${sessionId}/state?playerId=${playerId}`
  );
  return response.data;
}

/**
 * Start the game (creator only)
 * @param sessionId The session ID
 * @param playerId The player's ID
 */
export async function startGame(
  sessionId: string,
  playerId: string
): Promise<void> {
  await api.post(`/games/${sessionId}/start?playerId=${playerId}`);
}
