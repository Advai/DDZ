import { MiniCard } from './MiniCard';
import type { Card } from '../../services/types';

interface CenterPlayAreaProps {
  currentLead: {
    cards: Card[];
    comboType: string;
  } | null;
  phase: string;
  playerCount?: number;
  currentBet?: number;
  multiplier?: number;
  bombsPlayed?: number;
  rocketsPlayed?: number;
}

const formatComboType = (comboType: string) => {
  const typeMap: Record<string, string> = {
    SINGLE: '🎴 Single',
    PAIR: '👯 Pair',
    TRIPLE: '🎯 Triple',
    TRIPLE_PLUS_SINGLE: '🎯➕ Triple + Single',
    TRIPLE_PLUS_PAIR: '🎯➕➕ Triple + Pair',
    QUAD_PLUS_SINGLE: '💣 Quad + Singles',
    QUAD_PLUS_PAIR: '💣 Quad + Pairs',
    STRAIGHT: '📐 Straight',
    PAIR_STRAIGHT: '👯📐 Pair Straight',
    TRIPLE_STRAIGHT: '🎯📐 Triple Straight',
    AIRPLANE: '✈️ Airplane',
    AIRPLANE_PLUS_SINGLE: '✈️➕ Airplane + Singles',
    AIRPLANE_PLUS_PAIR: '✈️➕➕ Airplane + Pairs',
    BOMB: '💣 BOMB!',
    ROCKET: '🚀 ROCKET!',
  };
  return typeMap[comboType] || comboType;
};

export function CenterPlayArea({
  currentLead,
  phase,
  playerCount,
  currentBet,
  multiplier,
  bombsPlayed,
  rocketsPlayed,
}: CenterPlayAreaProps) {
  // Don't show center circle during bidding phase - the bidding panel has all the info
  if (phase === 'BIDDING') {
    return null;
  }

  return (
    <div
      className="absolute rounded-full flex flex-col items-center justify-center pointer-events-none"
      style={{
        left: '50%',
        top: '50%',
        transform: 'translate(-50%, -50%)',
        width: '360px',
        height: '360px',
        background: 'rgba(21, 20, 26, 0.8)',
        border: '2px solid rgba(248, 207, 44, 0.5)',
        zIndex: 5,
        padding: '20px',
      }}
    >
      {phase === 'LOBBY' ? (
        <div className="text-center">
          <div className="text-white text-4xl font-bold mb-2">Waiting for Players</div>
          <div className="text-yellow-300 text-7xl font-bold">
            {playerCount || 0} / 7
          </div>
        </div>
      ) : phase === 'PLAY' ? (
        <div className="flex flex-col items-center justify-center h-full px-2">
          {/* Top: Bet & Multiplier */}
          {(currentBet !== undefined || multiplier !== undefined) && (
            <div className="text-center mb-1">
              <div className="text-yellow-300 text-xl">Bet: {currentBet || 0}</div>
              <div className="text-yellow-300 text-2xl font-bold">
                Payout: {multiplier || 1}x
              </div>
            </div>
          )}

          {/* Middle: Bomb & Rocket Counts */}
          {(bombsPlayed !== undefined || rocketsPlayed !== undefined) && (
            <div className="flex gap-3 mb-1 text-lg text-white">
              <span>💣 Bombs: {bombsPlayed || 0}</span>
              <span>🚀 Rockets: {rocketsPlayed || 0}</span>
            </div>
          )}

          {/* Bottom: Lead Hand Cards */}
          {currentLead && currentLead.cards && currentLead.cards.length > 0 ? (
            <>
              <div className="text-yellow-300 text-xl mb-1">
                {formatComboType(currentLead.comboType)}
              </div>
              <div className="flex flex-wrap gap-1 justify-center max-w-full">
                {currentLead.cards.map((card, idx) => (
                  <MiniCard key={idx} card={card} />
                ))}
              </div>
            </>
          ) : (
            <div className="text-gray-400 text-lg mt-1">Waiting for play...</div>
          )}
        </div>
      ) : (
        <div className="text-center text-gray-400 text-2xl">
          Waiting for first play...
        </div>
      )}
    </div>
  );
}
