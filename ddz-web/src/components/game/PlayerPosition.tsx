import { MiniCard } from './MiniCard';
import type { Player } from '../../services/types';

interface PlayerPositionProps {
  player?: Player;
  isCurrentTurn: boolean;
  isLandlord: boolean;
  isCurrentUser: boolean;
  isEmpty: boolean;
  phase: string;
  onEmptySeatClick?: () => void;
}

export function PlayerPosition({
  player,
  isCurrentTurn,
  isLandlord,
  isCurrentUser,
  isEmpty,
  phase,
  onEmptySeatClick,
}: PlayerPositionProps) {
  const playerName = player?.name || player?.displayName || '';
  const cardCount = player?.cardCount || 0;
  const visibleCards = player?.visibleCards || [];
  const isConnected = player?.connected !== false;

  const baseHeight = 112; // Base height for player circle

  // Empty seat (only during LOBBY)
  if (isEmpty && phase === 'LOBBY') {
    return (
      <button
        onClick={onEmptySeatClick}
        className="w-28 h-28 rounded-full flex items-center justify-center transition-all"
        style={{
          border: '3px dashed #555',
          background: 'rgba(0,0,0,0.2)',
          cursor: 'pointer',
          zIndex: 10,
          position: 'relative',
        }}
        onMouseEnter={(e) => {
          e.currentTarget.style.borderColor = '#f8cf2c';
          e.currentTarget.style.background = 'rgba(248, 207, 44, 0.1)';
        }}
        onMouseLeave={(e) => {
          e.currentTarget.style.borderColor = '#555';
          e.currentTarget.style.background = 'rgba(0,0,0,0.2)';
        }}
      >
        <div className="text-gray-500 text-4xl">+</div>
      </button>
    );
  }

  // Occupied seat or empty during active game
  if (!isEmpty) {
    // Role-based colors: Blue for all players, Gold for current user
    const getPlayerColors = () => {
      if (isCurrentUser) {
        return {
          background: 'linear-gradient(135deg, #f8cf2c 0%, #ffdc4e 100%)',
          border: '#f8cf2c',
          shadow: '0 0 20px rgba(248, 207, 44, 0.8)',
        };
      }
      return {
        background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
        border: '#2563eb',
        shadow: '0 4px 12px rgba(37, 99, 235, 0.6)',
      };
    };

    const colors = getPlayerColors();

    return (
      <>
        <div
          className={`rounded-full flex flex-col items-center justify-center relative ${
            isCurrentTurn ? 'turn-indicator' : ''
          }`}
          style={{
            width: '112px',
            height: `${baseHeight}px`,
            background: colors.background,
            border: isCurrentTurn
              ? '3px solid #f8cf2c'
              : `3px solid ${colors.border}`,
            boxShadow: isCurrentTurn ? '0 0 20px rgba(248, 207, 44, 0.8)' : colors.shadow,
            opacity: isConnected ? 1 : 0.6,
            padding: '16px',
            transition: 'all 0.3s ease',
            zIndex: 8,
          }}
        >
        {/* Landlord crown */}
        {isLandlord && (
          <div
            className="absolute"
            style={{
              top: '-8px',
              right: '-8px',
              fontSize: '24px',
            }}
          >
            👑
          </div>
        )}

        {/* Player name and info */}
        <div className="text-center px-4 flex-shrink-0">
          <div
            className="text-xl font-bold truncate max-w-full"
            style={{
              color: isCurrentUser ? '#1a1a1a' : '#fff',
            }}
          >
            {playerName}
          </div>
          {isCurrentUser && (
            <div className="text-base font-bold text-gray-900">(You)</div>
          )}
        </div>

        {/* Card count badge */}
        {cardCount > 0 && (
          <div
            className="mt-1 px-3 py-2 rounded text-lg font-bold"
            style={{
              background: 'rgba(0,0,0,0.3)',
              color: '#f8cf2c',
            }}
          >
            {cardCount} {cardCount === 1 ? 'card' : 'cards'}
          </div>
        )}

        {/* Disconnected indicator */}
        {!isConnected && (
          <div
            className="absolute top-1 left-1 text-xs font-bold"
            style={{ color: '#ef4444' }}
          >
            🔴
          </div>
        )}
        </div>

        {/* Floating Landlord Cards - To the right of player circle */}
        {isLandlord && phase === 'PLAY' && visibleCards && visibleCards.length > 0 && (
          <div
            className="absolute flex flex-col gap-1 p-2 rounded-lg"
            style={{
              left: '120%',
              top: '50%',
              transform: 'translateY(-50%)',
              background: 'rgba(21, 20, 26, 0.95)',
              border: '2px solid rgba(248, 207, 44, 0.6)',
              boxShadow: '0 4px 12px rgba(0, 0, 0, 0.5)',
              zIndex: 25,
              minWidth: '50px',
            }}
          >
            {visibleCards.slice(0, 8).map((card, idx) => (
              <MiniCard key={idx} card={card} />
            ))}
            {visibleCards.length > 8 && (
              <div className="text-xs text-yellow-300 text-center">+{visibleCards.length - 8}</div>
            )}
          </div>
        )}
      </>
    );
  }

  // Empty seat during active game (should not be clickable)
  return (
    <div
      className="w-28 h-28 rounded-full flex items-center justify-center"
      style={{
        border: '3px dashed #333',
        background: 'rgba(0,0,0,0.1)',
      }}
    >
      <div className="text-gray-600 text-sm">Empty</div>
    </div>
  );
}
