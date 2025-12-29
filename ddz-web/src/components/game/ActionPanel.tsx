interface ActionPanelProps {
  phase: string;
  isMyTurn: boolean;
  selectedCardsCount: number;
  onPlay: () => void;
  onPass: () => void;
  onClear: () => void;
}

export function ActionPanel({
  phase,
  isMyTurn,
  selectedCardsCount,
  onPlay,
  onPass,
  onClear,
}: ActionPanelProps) {
  // Only show during PLAY phase when it's player's turn
  if (phase !== 'PLAY' || !isMyTurn) {
    return null;
  }

  return (
    <div
      className="flex flex-col gap-3"
      style={{
        position: 'sticky',
        top: '50vh',
        transform: 'translateY(-50%)',
        alignSelf: 'start',
        zIndex: 10,
      }}
    >
      <button
        onClick={onPlay}
        disabled={selectedCardsCount === 0}
        className="btn btn-primary px-12 py-6 text-3xl font-bold shadow-lg whitespace-nowrap"
        style={{ minWidth: '200px' }}
      >
        ▶️ Play
        {selectedCardsCount > 0 && ` (${selectedCardsCount})`}
      </button>

      <button
        onClick={onPass}
        className="btn btn-secondary px-12 py-6 text-3xl font-bold shadow-lg"
        style={{ minWidth: '200px' }}
      >
        ⏭️ Pass
      </button>

      {selectedCardsCount > 0 && (
        <button
          onClick={onClear}
          className="btn btn-secondary px-10 py-4 text-2xl shadow-lg"
          style={{ minWidth: '200px' }}
        >
          🗑️ Clear
        </button>
      )}
    </div>
  );
}
