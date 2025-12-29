import { PlayerPosition } from './PlayerPosition';
import { CenterPlayArea } from './CenterPlayArea';
import type { Player, Card } from '../../services/types';

interface CircularGameTableProps {
  players: Player[];
  currentPlayerId: string | null;
  currentLead: {
    cards: Card[];
    comboType: string;
  } | null;
  phase: string;
  currentUserId: string | null;
  landlordIds: string[];
  onSeatClick: (seatIndex: number) => void;
  currentBet?: number;
  multiplier?: number;
  bombsPlayed?: number;
  rocketsPlayed?: number;
}

export function CircularGameTable({
  players,
  currentPlayerId,
  currentLead,
  phase,
  currentUserId,
  landlordIds,
  onSeatClick,
  currentBet,
  multiplier,
  bombsPlayed,
  rocketsPlayed,
}: CircularGameTableProps) {
  // Create array of 7 seats
  const seats = Array.from({ length: 7 }, (_, seatIndex) => {
    // Find player in this seat
    const player = players.find(
      (p: Player) => (p.seatPosition ?? p.position) === seatIndex
    );

    const playerId = player?.id || player?.playerId;
    const isCurrentTurn = playerId === currentPlayerId;
    const isLandlord = landlordIds.includes(playerId || '');
    const isCurrentUser = playerId === currentUserId;
    const isEmpty = !player;

    return {
      seatIndex,
      player,
      playerId,
      isCurrentTurn,
      isLandlord,
      isCurrentUser,
      isEmpty,
    };
  });

  return (
    <div className="w-full flex items-center justify-center mb-6">
      {/* Perspective wrapper for 3D tilt */}
      <div
        style={{
          perspective: '1500px',
          perspectiveOrigin: 'center bottom',
        }}
      >
        <div className="relative game-table" style={{ width: '600px', height: '600px' }}>
          {/* Table surface - centered with 12° tilt */}
          <div
            className="absolute rounded-full shadow-2xl"
            style={{
              left: '50%',
              top: '50%',
              transform: 'translate(-50%, -50%) rotateX(12deg)',
              transformStyle: 'preserve-3d',
              width: '600px',
              height: '600px',
              background: 'radial-gradient(circle, #2d5016 0%, #1a3d0f 100%)',
              border: '12px solid #8b6f47',
              zIndex: 1,
            }}
          >
          {/* Center play area */}
          <CenterPlayArea
            currentLead={currentLead}
            phase={phase}
            playerCount={players.length}
            currentBet={currentBet}
            multiplier={multiplier}
            bombsPlayed={bombsPlayed}
            rocketsPlayed={rocketsPlayed}
          />
        </div>

        {/* Player seats positioned around table - all relative to center */}
        {seats.map(({ seatIndex, player, isCurrentTurn, isLandlord, isCurrentUser, isEmpty }) => {
          const angle = (seatIndex / 7) * 360 - 90;
          const radiusX = 48;
          const radiusY = 48;
          const x = 50 + radiusX * Math.cos((angle * Math.PI) / 180);
          const y = 50 + radiusY * Math.sin((angle * Math.PI) / 180);

          return (
            <div
              key={seatIndex}
              className="absolute"
              style={{
                left: `${x}%`,
                top: `${y}%`,
                transform: 'translate(-50%, -50%)',
                zIndex: 10,
              }}
            >
              <PlayerPosition
                player={player}
                isCurrentTurn={isCurrentTurn}
                isLandlord={isLandlord}
                isCurrentUser={isCurrentUser}
                isEmpty={isEmpty}
                phase={phase}
                onEmptySeatClick={() => onSeatClick(seatIndex)}
              />
            </div>
          );
        })}
        </div>
      </div>
    </div>
  );
}
