import Svg, { Path } from 'react-native-svg';

type Props = {
  size?: number;
  fill?: string;
};

export default function ChevronRightIcon({ size = 16, fill = 'currentColor' }: Props) {
  return (
    <Svg viewBox="0 0 16 16" width={size} height={size} fill={fill}>
      <Path d="M6.22 3.22a.75.75 0 0 1 1.06 0l4.25 4.25a.75.75 0 0 1 0 1.06l-4.25 4.25a.75.75 0 0 1-1.06-1.06L9.94 8 6.22 4.28a.75.75 0 0 1 0-1.06Z" />
    </Svg>
  );
}
