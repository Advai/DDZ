import { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { login, joinGame, startGame } from '../services/api';
import type { GameInfo, Card } from '../services/types';
import { CircularGameTable } from '../components/game/CircularGameTable';
import { PlayerHand } from '../components/game/PlayerHand';
import { ActionPanel } from '../components/game/ActionPanel';
import { LeaderboardModal } from '../components/game/LeaderboardModal';

interface GameState {
  phase: string;
  players: any[];
  currentPlayer: string | null;
  myHand: Card[];
  playerCount: number;
  maxBid: number;
  currentBet?: number;
  landlordIds?: string[];
  currentLead?: {
    cards: Card[];
    comboType: string;
  };
  awaitingLandlordSelection?: string;
  bombsPlayed?: number;
  rocketsPlayed?: number;
  multiplier?: number;
  isPaused?: boolean;
  scores?: Record<string, number>;
}

/**
 * Game page - lobby and active game with WebSocket
 * Ported from MVP design
 */
export function GamePage() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const navigate = useNavigate();
  const wsRef = useRef<WebSocket | null>(null);

  const [gameInfo, setGameInfo] = useState<GameInfo | null>(null);
  const [gameState, setGameState] = useState<GameState | null>(null);
  const [showJoinModal, setShowJoinModal] = useState(false);
  const [nickname, setNickname] = useState('');
  const [joinError, setJoinError] = useState('');
  const [isJoining, setIsJoining] = useState(false);
  const [linkCopied, setLinkCopied] = useState(false);
  const [connected, setConnected] = useState(false);
  const [selectedCards, setSelectedCards] = useState<Set<string>>(new Set());
  const [playerNames, setPlayerNames] = useState<Record<string, string>>({});
  const [selectedSeat, setSelectedSeat] = useState<number | null>(null);
  const [isLoadingGame, setIsLoadingGame] = useState(true);
  const [gameLoadError, setGameLoadError] = useState<string | null>(null);
  const [gameError, setGameError] = useState<string | null>(null);
  const [showLeaderboard, setShowLeaderboard] = useState(false);
  const [playerId, setPlayerId] = useState<string | null>(
    sessionId ? localStorage.getItem(`playerId_${sessionId}`) : null
  );

  const userId = localStorage.getItem('userId');
  const currentDisplayName = localStorage.getItem('displayName');

  // WebSocket connection
  useEffect(() => {
    if (!sessionId) return;

    loadGameState();

    // Connect WebSocket (will reconnect if playerId changes from null to actual ID)
    connectWebSocket();

    return () => {
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, [sessionId, playerId]);

  const connectWebSocket = () => {
    if (!sessionId) {
      console.log('Cannot connect WebSocket - missing sessionId');
      return;
    }

    // Make playerId optional in URL to support spectators
    const wsUrl = playerId
      ? `ws://localhost:8080/ws/game/${sessionId}?playerId=${playerId}`
      : `ws://localhost:8080/ws/game/${sessionId}`;
    console.log('Connecting to WebSocket:', wsUrl, playerId ? '(with playerId)' : '(spectator mode)');

    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      console.log('✓ WebSocket connected successfully');
      setConnected(true);
    };

    ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      console.log('WebSocket message received:', message);

      if (message.error) {
        console.error('WebSocket error:', message.error);
        return;
      }

      if (message.type === 'GAME_UPDATE') {
        if (message.state) {
          // Full game state (for seated players)
          console.log('Updating game state from WebSocket');
          setGameState(message.state);

          // Update player names mapping
          if (message.state.players) {
            const names: Record<string, string> = {};
            message.state.players.forEach((p: any) => {
              names[p.id] = p.name;
              console.log(`Player ${p.id}: ${p.name}`);
            });
            setPlayerNames(names);
          }
        } else if (message.spectatorInfo) {
          // Basic info (for spectators)
          console.log('Updating game info from spectator WebSocket');
          setGameInfo((prev: any) => ({
            ...prev,
            players: message.spectatorInfo.players,
            phase: message.spectatorInfo.phase
          }));
        }
      }
    };

    ws.onclose = () => {
      console.log('✗ WebSocket disconnected');
      setConnected(false);
    };

    ws.onerror = (error) => {
      console.error('✗ WebSocket error:', error);
      setConnected(false);
    };
  };

  const sendWebSocketMessage = (message: any) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      console.log('Sending WebSocket message:', message);
      wsRef.current.send(JSON.stringify(message));
    } else {
      console.error('WebSocket not connected');
    }
  };

  const loadGameState = async () => {
    if (!sessionId) {
      setGameLoadError('No session ID provided');
      setIsLoadingGame(false);
      return;
    }

    setIsLoadingGame(true);
    setGameLoadError(null);
    try {
      // Always fetch basic game info first
      const response = await fetch(`/api/games/${sessionId}`);
      if (response.ok) {
        const state = await response.json();
        setGameInfo(state);
        console.log('Loaded game info:', state);
      } else {
        const errorText = await response.text();
        setGameLoadError(`Failed to load game: ${response.status} - ${errorText}`);
        console.error('Failed to fetch game info:', response.status, errorText);
      }
    } catch (err: any) {
      setGameLoadError(`Error loading game: ${err.message || 'Unknown error'}`);
      console.error('Failed to load game state:', err);
    } finally {
      setIsLoadingGame(false);
    }
  };

  const handleJoinGame = async () => {
    if (!sessionId) return;

    const trimmedNickname = nickname.trim();
    if (!trimmedNickname) {
      setJoinError('Please enter a nickname');
      return;
    }

    setIsJoining(true);
    setJoinError('');

    try {
      const username = trimmedNickname.toLowerCase().replace(/[^a-z0-9]/g, '');
      const loginResponse = await login(username, trimmedNickname);

      localStorage.setItem('userId', loginResponse.userId);
      localStorage.setItem('username', loginResponse.username);
      localStorage.setItem('displayName', loginResponse.displayName);

      // Check if we have a creatorToken for this session
      const creatorToken = localStorage.getItem('creatorToken');
      const creatorSessionId = localStorage.getItem('creatorGameId'); // Note: keeping same key for now
      const tokenToUse = (creatorSessionId === sessionId) ? creatorToken : undefined;

      const gameInfo = await joinGame(
        sessionId,
        trimmedNickname,
        loginResponse.userId,
        selectedSeat ?? undefined,
        tokenToUse ?? undefined
      );

      // Store the player ID for WebSocket connection (per-session)
      if (gameInfo.yourPlayerId && sessionId) {
        localStorage.setItem(`playerId_${sessionId}`, gameInfo.yourPlayerId);
        setPlayerId(gameInfo.yourPlayerId); // Update state to trigger WebSocket connection
      }

      // Clear creatorToken after using it
      if (tokenToUse) {
        localStorage.removeItem('creatorToken');
        localStorage.removeItem('creatorGameId');
      }

      // Update local state with new game info
      setGameInfo(gameInfo);
      setShowJoinModal(false);
      setSelectedSeat(null);
      setNickname('');
      setIsJoining(false);

      // WebSocket will connect automatically via useEffect watching playerId
    } catch (err) {
      console.error('Failed to join game:', err);
      setJoinError('Failed to join game. Please try again.');
      setIsJoining(false);
    }
  };

  const handleCopyLink = () => {
    const gameUrl = window.location.href;
    navigator.clipboard.writeText(gameUrl);
    setLinkCopied(true);
    setTimeout(() => setLinkCopied(false), 2000);
  };

  const handleStartGame = async () => {
    if (!sessionId || !playerId) {
      setGameError('Cannot start game - missing session ID or player ID');
      return;
    }

    setGameError(null); // Clear previous errors

    try {
      // Use the same startGame endpoint for both initial start and restart
      await startGame(sessionId, playerId);
      console.log('Game started successfully');
    } catch (error: any) {
      console.error('Error starting game:', error);

      // Properly extract error message
      let errorMsg = 'Failed to start game';
      if (error.response?.data) {
        errorMsg = typeof error.response.data === 'string'
          ? error.response.data
          : error.response.data.message || JSON.stringify(error.response.data);
      }

      setGameError(errorMsg);
    }
  };

  const placeBid = (bidValue: number) => {
    sendWebSocketMessage({
      type: 'BID',
      playerId: playerId,
      bidValue: bidValue,
    });
  };

  const selectLandlord = (selectedPlayerId: string) => {
    sendWebSocketMessage({
      type: 'SELECT_LANDLORD',
      playerId: playerId,
      selectedPlayerId: selectedPlayerId,
    });
  };

  const toggleCardSelection = (cardKey: string) => {
    const newSelected = new Set(selectedCards);
    if (newSelected.has(cardKey)) {
      newSelected.delete(cardKey);
    } else {
      newSelected.add(cardKey);
    }
    setSelectedCards(newSelected);
  };

  const playSelectedCards = () => {
    if (selectedCards.size === 0) {
      alert('Please select cards to play');
      return;
    }

    // Extract indices and get actual card objects
    const indices = Array.from(selectedCards).map(key => {
      const lastUnderscoreIndex = key.lastIndexOf('_');
      return parseInt(key.substring(lastUnderscoreIndex + 1));
    });

    const sortedHand = sortCards(gameState?.myHand || []);
    const cards = indices.map(idx => sortedHand[idx]);

    sendWebSocketMessage({
      type: 'PLAY',
      playerId: playerId,
      cards: cards,
    });

    setSelectedCards(new Set());
  };

  const pass = () => {
    sendWebSocketMessage({
      type: 'PASS',
      playerId: playerId,
    });
  };

  const sortCards = (cards: Card[]) => {
    const rankOrder = [
      'THREE',
      'FOUR',
      'FIVE',
      'SIX',
      'SEVEN',
      'EIGHT',
      'NINE',
      'TEN',
      'JACK',
      'QUEEN',
      'KING',
      'ACE',
      'TWO',
      'LITTLE_JOKER',
      'BIG_JOKER',
    ];
    return [...cards].sort((a, b) => rankOrder.indexOf(a.rank) - rankOrder.indexOf(b.rank));
  };

  const isMyTurn = gameState?.currentPlayer === playerId;
  const canBid = gameState?.phase === 'BIDDING' && isMyTurn && !gameState?.awaitingLandlordSelection;
  const showLandlordSelection =
    gameState?.phase === 'BIDDING' && gameState?.awaitingLandlordSelection === playerId;
  const isLandlord = gameState?.landlordIds?.includes(playerId || '');

  // Get player name from backend game state instead of localStorage
  const myPlayerInfo = gameState?.players?.find((p: any) => p.id === playerId);
  const myActualName = myPlayerInfo?.name || currentDisplayName || 'Unknown';

  // Debug logging
  useEffect(() => {
    console.log('[GamePage Debug]', {
      sessionId,
      playerId,
      userId,
      hasGameInfo: !!gameInfo,
      gameInfoPhase: gameInfo?.phase,
      hasGameState: !!gameState,
      gameStatePhase: gameState?.phase,
      shouldShowTable: (gameInfo?.phase === 'LOBBY' || gameState?.phase === 'LOBBY' || (!gameInfo?.phase && !gameState?.phase)),
      gameInfoPlayers: gameInfo?.players?.length || 0,
      gameStatePlayers: gameState?.players?.length || 0
    });
  }, [sessionId, playerId, userId, gameInfo, gameState]);

  // Auto-clear errors when game starts/progresses
  useEffect(() => {
    if (gameState?.phase !== 'TERMINATED' && gameState?.phase !== 'LOBBY') {
      setGameError(null);
    }
  }, [gameState?.phase]);

  if (!sessionId) {
    return <div className="p-8 text-white">Invalid session ID</div>;
  }

  return (
    <div className="min-h-screen p-5">
      <h1
        className="text-center text-5xl mb-8 font-bold"
        style={{ textShadow: '2px 2px 4px rgba(0,0,0,0.5)' }}
      >
        🃏 Dou Dizhu (斗地主) 🃏
      </h1>

      <div style={{
        display: 'grid',
        gridTemplateColumns: '1fr auto',
        gridTemplateRows: 'auto 1fr auto',
        gridTemplateAreas: `
          "info actions"
          "arena actions"
          "hand actions"
        `,
        gap: '30px',
        maxWidth: '1400px',
        margin: '0 auto',
        minHeight: 'calc(100vh - 200px)',
        padding: '20px',
      }}>

        {/* Join Modal */}
        {showJoinModal && (
          <div
            className="fixed inset-0 flex items-center justify-center p-4"
            style={{ background: 'rgba(21, 20, 26, 0.95)', zIndex: 1000 }}
          >
            <div
              className="w-full max-w-md p-8 rounded-xl"
              style={{
                background: 'rgba(51, 81, 85, 0.4)',
                backdropFilter: 'blur(10px)',
                border: '1px solid rgba(248, 207, 44, 0.3)',
              }}
            >
              <h2 className="text-2xl font-bold mb-6" style={{ color: '#f8cf2c' }}>
                Join Game
              </h2>

              <div className="mb-4">
                <p className="text-white mb-4">
                  Enter your nickname to claim a seat at the table:
                </p>

                <label className="block mb-2 font-medium text-white">Your Nickname</label>
                <input
                  type="text"
                  placeholder="Your Nickname"
                  value={nickname}
                  onChange={(e) => {
                    setNickname(e.target.value);
                    setJoinError('');
                  }}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      handleJoinGame();
                    }
                  }}
                  className="w-full px-3 py-2 rounded-md border"
                  style={{
                    border: '1px solid rgba(255, 255, 255, 0.3)',
                    background: 'rgba(255, 255, 255, 0.9)',
                    color: '#333',
                  }}
                  autoFocus
                />
                {joinError && (
                  <p className="mt-2 text-sm" style={{ color: '#ab202a' }}>
                    {joinError}
                  </p>
                )}
              </div>

              <button
                onClick={handleJoinGame}
                disabled={isJoining}
                className="btn btn-primary w-full px-4 py-3"
              >
                {isJoining ? 'Joining...' : 'Join Game'}
              </button>

              <button
                onClick={() => {
                  setShowJoinModal(false);
                  setNickname('');
                  setJoinError('');
                }}
                className="btn btn-secondary w-full mt-3 px-4 py-2"
              >
                Cancel
              </button>
            </div>
          </div>
        )}

        {/* Game Info - Direct child of grid */}
        {!showJoinModal && userId && gameState && gameState.phase !== 'LOBBY' && (
          <div style={{ gridArea: 'info' }}>
            {/* Connection Status */}
            {playerId && (
              <div
                className="text-center py-2 px-4 rounded-md mb-3 font-bold"
                style={{
                  background: connected
                    ? 'rgba(248, 207, 44, 0.3)'
                    : 'rgba(171, 32, 42, 0.3)',
                  color: connected ? '#f8cf2c' : '#ab202a',
                }}
              >
                {connected ? '✓ Connected' : '✗ Disconnected'}
              </div>
            )}

            {/* Paused Banner */}
            {gameState?.isPaused && (
              <div
                className="text-center py-3 px-5 rounded-lg mb-3 font-bold"
                style={{
                  background: 'rgba(248, 207, 44, 0.9)',
                  color: '#15141a',
                  border: '2px solid #f8cf2c',
                }}
              >
                ⏸️ Game Paused - Waiting for reconnect
              </div>
            )}

            {/* Game Error Banner */}
            {gameError && (
              <div
                className="mb-3 p-3 rounded-lg"
                style={{
                  background: 'rgba(239, 68, 68, 0.2)',
                  border: '2px solid #ef4444',
                }}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="text-xl">⚠️</span>
                    <p className="text-sm font-bold text-red-400">{gameError}</p>
                  </div>
                  <button
                    onClick={() => setGameError(null)}
                    className="text-red-400 hover:text-red-300 text-lg font-bold"
                  >
                    ✕
                  </button>
                </div>
              </div>
            )}

            {/* Game Info Content */}
            <div
              className="rounded-lg p-6"
              style={{
                background: 'rgba(255, 255, 255, 0.08)',
                backdropFilter: 'blur(10px)',
                border: '1px solid rgba(248, 207, 44, 0.2)',
                boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
                zIndex: 15,
                paddingBottom: '40px',
              }}
            >
              {/* Row 1: Name, Phase, Role in 3-column grid */}
              <div className="grid grid-cols-3 gap-4 mb-3">
                <div
                  className="p-4 rounded-lg"
                  style={{
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid rgba(248, 207, 44, 0.15)',
                  }}
                >
                  <div className="text-sm text-gray-300 mb-1">Your Name</div>
                  <div className="font-bold text-white text-lg">{myActualName}</div>
                </div>

                <div
                  className="p-4 rounded-lg"
                  style={{
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid rgba(248, 207, 44, 0.15)',
                  }}
                >
                  <div className="text-sm text-gray-300 mb-1">Phase</div>
                  <div className="font-bold text-white text-lg">{gameState.phase}</div>
                </div>

                {isLandlord !== undefined && gameState.landlordIds && gameState.landlordIds.length > 0 && (
                  <div
                    className="p-4 rounded-lg"
                    style={{
                      background: 'rgba(255, 255, 255, 0.05)',
                      border: '1px solid rgba(248, 207, 44, 0.15)',
                    }}
                  >
                    <div className="text-sm text-gray-300 mb-1">Your Role</div>
                    <div
                      className="font-bold text-lg"
                      style={{ color: isLandlord ? '#f8cf2c' : '#fff' }}
                    >
                      {isLandlord ? '👑 Landlord' : '🌾 Peasant'}
                    </div>
                  </div>
                )}
              </div>

              {/* Row 2: Copy Link and Leaderboard buttons side by side */}
              <div className="grid grid-cols-2 gap-3">
                <button
                  onClick={handleCopyLink}
                  className="p-3 rounded-lg transition-all cursor-pointer hover:scale-[1.02]"
                  style={{
                    background: linkCopied
                      ? 'rgba(34, 197, 94, 0.3)'
                      : 'rgba(248, 207, 44, 0.3)',
                    border: linkCopied
                      ? '2px solid rgba(34, 197, 94, 0.6)'
                      : '2px solid rgba(248, 207, 44, 0.6)',
                  }}
                >
                  <div className="text-center text-sm font-semibold" style={{ color: linkCopied ? '#22c55e' : '#f8cf2c' }}>
                    {linkCopied ? '✓ Copied!' : '📋 Copy Link'}
                  </div>
                </button>

                <button
                  onClick={() => setShowLeaderboard(true)}
                  className="p-3 rounded-lg transition-all cursor-pointer hover:scale-[1.02]"
                  style={{
                    background: 'rgba(248, 207, 44, 0.3)',
                    border: '2px solid rgba(248, 207, 44, 0.6)',
                  }}
                >
                  <div className="text-center text-sm font-semibold" style={{ color: '#f8cf2c' }}>
                    📊 Leaderboard
                  </div>
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Arena - Direct child of grid */}
        {!showJoinModal && (
          <div style={{
            gridArea: 'arena',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '15px',
            paddingTop: '40px',
          }}>
            {/* Error State */}
            {gameLoadError && (
              <div className="py-3 px-5 rounded-lg" style={{
                background: 'rgba(171, 32, 42, 0.2)',
                border: '2px solid #ab202a'
              }}>
                <p className="text-sm font-bold text-red-400">{gameLoadError}</p>
              </div>
            )}

            {/* Loading State */}
            {isLoadingGame && !gameLoadError && (
              <div className="text-center py-8">
                <div className="text-white text-xl">Loading game...</div>
              </div>
            )}

            {/* Instructions for non-joined players (in lobby) */}
            {!playerId && gameInfo && gameInfo.phase === 'LOBBY' && !isLoadingGame && (
              <div
                className="text-center py-3 px-5 rounded-lg"
                style={{
                  background: 'rgba(248, 207, 44, 0.2)',
                  border: '2px solid #f8cf2c',
                }}
              >
                <p className="text-sm font-bold" style={{ color: '#f8cf2c' }}>
                  Click on any empty seat (+) to join the game
                </p>
              </div>
            )}

            {/* Spectating message (not in lobby) */}
            {!playerId && gameInfo && gameInfo.phase !== 'LOBBY' && !isLoadingGame && (
              <div
                className="text-center py-3 px-5 rounded-lg"
                style={{
                  background: 'rgba(248, 207, 44, 0.2)',
                  border: '2px solid #f8cf2c',
                }}
              >
                <p className="text-sm text-white">Spectating...</p>
              </div>
            )}

            {/* Circular Game Table - ALL PHASES */}
            {!isLoadingGame && (gameInfo || gameState) && (
              <CircularGameTable
                players={gameState?.players || gameInfo?.players || []}
                currentPlayerId={gameState?.currentPlayer || null}
                currentLead={gameState?.currentLead || null}
                phase={gameState?.phase || gameInfo?.phase || 'LOBBY'}
                currentUserId={userId}
                landlordIds={gameState?.landlordIds || []}
                currentBet={gameState?.currentBet}
                multiplier={gameState?.multiplier}
                bombsPlayed={gameState?.bombsPlayed}
                rocketsPlayed={gameState?.rocketsPlayed}
                onSeatClick={(seatIndex) => {
                  setSelectedSeat(seatIndex);
                  setShowJoinModal(true);
                }}
              />
            )}
          </div>
        )}

        {/* Player Hand - Direct child of grid */}
        {!showJoinModal && gameState?.myHand && gameState.myHand.length > 0 && (
          <div style={{ gridArea: 'hand' }}>
            <PlayerHand
              cards={gameState.myHand}
              selectedCards={selectedCards}
              onCardToggle={toggleCardSelection}
              canSelect={gameState.phase === 'PLAY' && isMyTurn}
            />
          </div>
        )}

        {/* Action Panel - Direct child of grid */}
        {!showJoinModal && (
          <div style={{ gridArea: 'actions' }}>
            <ActionPanel
              phase={gameState?.phase || ''}
              isMyTurn={isMyTurn}
              selectedCardsCount={selectedCards.size}
              onPlay={playSelectedCards}
              onPass={pass}
              onClear={() => setSelectedCards(new Set())}
            />
          </div>
        )}

        {/* Non-grid elements - Bidding Panel */}
        {!showJoinModal && gameState?.phase === 'BIDDING' && !showLandlordSelection && (
          <div
            className="p-5 rounded-lg"
            style={{
              background: 'rgba(51, 81, 85, 0.4)',
              border: '2px solid rgba(248, 207, 44, 0.3)',
              gridColumn: '1 / -1',
            }}
          >
            <h3 className="text-xl font-bold mb-3" style={{ color: '#f8cf2c' }}>
              💰 Bidding Phase
            </h3>
            <p className="mb-4 text-lg" style={{ color: canBid ? '#f8cf2c' : '#fff', fontWeight: canBid ? 'bold' : 'normal' }}>
              {canBid
                ? `👉 YOUR TURN - Place your bid (0 = pass, 1-${gameState.maxBid || 3} = bid value):`
                : gameState.awaitingLandlordSelection
                ? '⏳ Waiting for landlord selection...'
                : '⏳ Waiting for other players to bid...'}
            </p>
            <div className="grid grid-cols-4 gap-3">
              <button
                onClick={() => placeBid(0)}
                disabled={!canBid}
                className="btn btn-secondary px-3 py-2"
              >
                Pass (0)
              </button>
              {Array.from({ length: gameState.maxBid || 3 }, (_, i) => i + 1).map((bid) => (
                <button
                  key={bid}
                  onClick={() => placeBid(bid)}
                  disabled={!canBid}
                  className="btn btn-primary px-3 py-2"
                >
                  Bid {bid}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Non-grid elements - Landlord Selection Panel */}
        {!showJoinModal && showLandlordSelection && (
          <div
            className="p-5 rounded-lg"
            style={{
              background: 'rgba(248, 207, 44, 0.4)',
              border: '2px solid #f8cf2c',
              gridColumn: '1 / -1',
            }}
          >
            <h3 className="text-xl font-bold mb-3" style={{ color: '#f8cf2c' }}>
              👑 You Are Primary Landlord - Select Co-Landlord
            </h3>
            <p className="mb-4 text-lg">Choose another player to be your co-landlord:</p>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {gameState.players
                .filter(
                  (p: any) =>
                    p.id !== playerId && !gameState.landlordIds?.includes(p.id)
                )
                .map((player: any) => (
                  <button
                    key={player.id}
                    onClick={() => selectLandlord(player.id)}
                    className="btn btn-primary px-6 py-3 text-lg"
                  >
                    Select {player.name}
                  </button>
                ))}
            </div>
          </div>
        )}

        {/* Non-grid elements - Game Over */}
        {!showJoinModal && gameState?.phase === 'TERMINATED' && gameState.scores && (
          <div
            className="p-5 rounded-lg"
            style={{
              background: 'rgba(248, 207, 44, 0.3)',
              border: '2px solid #f8cf2c',
              gridColumn: '1 / -1',
            }}
          >
            <h3 className="text-2xl font-bold mb-4 text-center" style={{ color: '#f8cf2c' }}>
              🎉 GAME OVER 🎉
            </h3>
            <div className="space-y-2">
              {Object.entries(gameState.scores).map(([playerId, score]) => {
                const playerName = playerNames[playerId] || 'Unknown';
                const color = score > 0 ? '#f8cf2c' : score < 0 ? '#ab202a' : '#fff';
                const icon = score > 0 ? '👑' : '💀';
                return (
                  <div key={playerId} className="text-lg" style={{ color }}>
                    {icon} {playerName}: {score > 0 ? '+' : ''}
                    {score}
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Non-grid elements - Start/Leave Buttons */}
        {!showJoinModal && (gameState?.phase === 'LOBBY' || gameState?.phase === 'TERMINATED' ||
          (!gameState && (gameInfo?.phase === 'LOBBY' || gameInfo?.phase === 'TERMINATED'))) && (
          <div style={{ gridColumn: '1 / -1', display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {/* Start Game Button - Available to all players */}
            <button
              onClick={handleStartGame}
              className="btn btn-primary w-full px-4 py-4 text-xl"
            >
              {(gameState?.phase === 'TERMINATED' || gameInfo?.phase === 'TERMINATED') ? '🎮 Play Again' : '🎮 Start Game'}
            </button>

            {/* Leave Game Button */}
            <button onClick={() => navigate('/')} className="btn btn-danger w-full px-4 py-2">
              Leave Game
            </button>
          </div>
        )}

        {/* Leaderboard Modal */}
        <LeaderboardModal
          isOpen={showLeaderboard}
          onClose={() => setShowLeaderboard(false)}
          gameId={sessionId || ''}
        />
      </div>
    </div>
  );
}
