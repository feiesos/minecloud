import Svg, { Path } from 'react-native-svg';

type Props = {
  size?: number;
  fill?: string;
};

export default function DownloadIcon({ size = 18, fill = 'currentColor' }: Props) {
  return (
    <Svg viewBox="0 0 16 16" width={size} height={size} fill={fill}>
      <Path d="M2.75 14A1.75 1.75 0 0 1 1 12.25v-2.5a.75.75 0 0 1 1.5 0v2.5c0 .138.112.25.25.25h10.5a.25.25 0 0 0 .25-.25v-2.5a.75.75 0 0 1 1.5 0v2.5A1.75 1.75 0 0 1 13.25 14H2.75ZM8 1a.75.75 0 0 1 .75.75v7.19l1.72-1.72a.75.75 0 1 1 1.06 1.06l-3 3a.75.75 0 0 1-1.06 0l-3-3a.75.75 0 1 1 1.06-1.06l1.72 1.72V1.75A.75.75 0 0 1 8 1Z" />
    </Svg>
  );
}
