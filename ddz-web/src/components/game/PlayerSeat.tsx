interface PlayerSeatProps {
  /** Seat number (1-indexed) */
  seatNumber: number;
  /** Player display name if seat is occupied */
  playerName?: string;
  /** Whether this seat is occupied */
  isOccupied: boolean;
  /** Callback when "Sit Here" is clicked */
  onSit?: () => void;
  /** Whether the current user is in this seat */
  isCurrentUser?: boolean;
}

/**
 * Player seat component with polished, modern styling.
 * Matches MVP aesthetic with gradients and professional look.
 */
export function PlayerSeat({
  seatNumber,
  playerName,
  isOccupied,
  onSit,
  isCurrentUser = false,
}: PlayerSeatProps) {
  return (
    <div className="flex flex-col items-center space-y-3">
      {/* Seat content */}
      <div className="w-32 h-32 sm:w-36 sm:h-36 flex items-center justify-center">
        {isOccupied ? (
          // Occupied seat - professional avatar with gradient border
          <div className="relative w-full h-full">
            <div
              className={`absolute inset-0 rounded-full ${
                isCurrentUser
                  ? 'bg-gradient-to-br from-amber-400 to-yellow-600'
                  : 'bg-gradient-to-br from-gray-600 to-gray-800'
              } p-1`}
            >
              <div className="w-full h-full rounded-full bg-gradient-to-br from-gray-800 to-gray-900 flex items-center justify-center">
                <div className="text-center px-3">
                  <div className={`text-lg font-bold truncate ${
                    isCurrentUser ? 'text-amber-400' : 'text-white'
                  }`}>
                    {playerName}
                  </div>
                </div>
              </div>
            </div>
            {isCurrentUser && (
              <div className="absolute -bottom-2 left-1/2 transform -translate-x-1/2 bg-gradient-to-r from-amber-400 to-yellow-600 text-gray-900 text-xs font-bold px-3 py-1 rounded-full shadow-lg">
                YOU
              </div>
            )}
          </div>
        ) : (
          // Empty seat - dashed border with hover effect
          <button
            onClick={onSit}
            className="w-full h-full rounded-full border-4 border-dashed border-gray-700 hover:border-green-500 hover:bg-green-500/10 transition-all duration-200 flex flex-col items-center justify-center text-gray-500 hover:text-green-500 font-bold shadow-lg hover:shadow-green-500/50"
          >
            <div className="text-3xl mb-1">+</div>
            <div className="text-sm">SIT</div>
          </button>
        )}
      </div>

      {/* Seat label */}
      <div className="bg-gray-800 px-4 py-1 rounded-full border border-gray-700 shadow-md">
        <span className="text-gray-400 text-sm font-semibold">
          Seat {seatNumber}
        </span>
      </div>
    </div>
  );
}
