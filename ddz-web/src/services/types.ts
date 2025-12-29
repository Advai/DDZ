/**
 * TypeScript interfaces for DDZ game API.
 * These types match the backend Java models.
 */

export interface Card {
  rank: string;
  suit: string;
}

export interface User {
  userId: string;
  username: string;
  displayName: string;
}

export interface Player {
  playerId: string;
  displayName: string;
  connected: boolean;
  cardCount: number;
  position?: number;
  seatPosition?: number;
  id?: string; // Server uses 'id' instead of 'playerId'
  name?: string; // Server uses 'name' instead of 'displayName'
  visibleCards?: Card[]; // Landlords' cards (visible to all players)
}

export interface GameInfo {
  gameId: string;
  joinCode?: string;
  phase: 'LOBBY' | 'BIDDING' | 'PLAY' | 'TERMINATED';
  playerCount: number;
  creatorId?: string;
  yourPlayerId?: string;
  players: Player[];
}

export interface LoginRequest {
  username: string;
  displayName: string;
}

export interface LoginResponse {
  userId: string;
  username: string;
  displayName: string;
}

export interface CreateGameRequest {
  playerCount: number;
}

export interface CreateGameResponse {
  sessionId: string;
  gameId: string;
  joinCode: string;
  creatorToken: string;
}

export interface JoinGameRequest {
  playerName: string;
  userId: string;
  seatPosition?: number;
  creatorToken?: string;
}

export interface JoinGameResponse {
  playerId: string;
  displayName: string;
}
