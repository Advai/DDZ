import type { Card } from '../../services/types';

interface MiniCardProps {
  card: Card;
}

export function MiniCard({ card }: MiniCardProps) {
  const isJoker = card.rank === 'LITTLE_JOKER' || card.rank === 'BIG_JOKER';
  const isRed = card.suit === 'HEARTS' || card.suit === 'DIAMONDS';

  const formatRank = (rank: string) => {
    const rankMap: Record<string, string> = {
      THREE: '3',
      FOUR: '4',
      FIVE: '5',
      SIX: '6',
      SEVEN: '7',
      EIGHT: '8',
      NINE: '9',
      TEN: '10',
      JACK: 'J',
      QUEEN: 'Q',
      KING: 'K',
      ACE: 'A',
      TWO: '2',
      LITTLE_JOKER: '🃏',
      BIG_JOKER: '🤡',
    };
    return rankMap[rank] || rank;
  };

  return (
    <div
      className={`mini-card ${isJoker ? 'joker' : isRed ? 'red' : 'black'}`}
    >
      {isJoker ? (
        <div className="rank" style={{ fontSize: '20px' }}>
          {formatRank(card.rank)}
        </div>
      ) : (
        <>
          <div className="rank" style={{ fontSize: '14px' }}>
            {formatRank(card.rank)}
          </div>
          <div className="suit" style={{ fontSize: '14px' }}>
            {card.suit === 'HEARTS'
              ? '♥'
              : card.suit === 'DIAMONDS'
              ? '♦'
              : card.suit === 'CLUBS'
              ? '♣'
              : '♠'}
          </div>
        </>
      )}
    </div>
  );
}
