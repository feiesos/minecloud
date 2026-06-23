import { useState, useCallback } from 'react';
import { StyleSheet, FlatList, TextInput, Pressable, ActivityIndicator, Keyboard } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ThemedView } from '@/components/themed-view';
import { ThemedText } from '@/components/themed-text';
import { Spacing, MaxContentWidth } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { searchFiles, type SearchResult } from '@/api/files';

export default function SearchScreen() {
  const theme = useTheme();
  const [query, setQuery] = useState('');
  const [type, setType] = useState<'all' | 'file' | 'folder'>('all');
  const [results, setResults] = useState<SearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const [error, setError] = useState('');

  const handleSearch = useCallback(async () => {
    const q = query.trim();
    if (!q) return;
    Keyboard.dismiss();
    setLoading(true);
    setError('');
    setSearched(true);
    try {
      const res = await searchFiles(q, type === 'all' ? undefined : type);
      setResults(res.items);
    } catch (err) {
      setError(err instanceof Error ? err.message : '搜索失败');
    } finally {
      setLoading(false);
    }
  }, [query, type]);

  function formatSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return `${(bytes / Math.pow(1024, i)).toFixed(i > 0 ? 1 : 0)} ${units[i]}`;
  }

  function renderItem({ item }: { item: SearchResult }) {
    return (
      <ThemedView
        style={[styles.item, { borderBottomColor: theme.textSecondary + '30' }]}
      >
        <ThemedText style={styles.itemIcon}>{item.isDir ? '📁' : '📄'}</ThemedText>
        <ThemedView style={styles.itemInfo}>
          <ThemedText style={styles.itemName} numberOfLines={1}>{item.name}</ThemedText>
          <ThemedText style={styles.itemPath} numberOfLines={1}>{item.path}</ThemedText>
        </ThemedView>
        {!item.isDir && (
          <ThemedText style={styles.itemSize}>{formatSize(item.size)}</ThemedText>
        )}
      </ThemedView>
    );
  }

  const filters: { key: typeof type; label: string }[] = [
    { key: 'all', label: '全部' },
    { key: 'file', label: '文件' },
    { key: 'folder', label: '文件夹' },
  ];

  return (
    <SafeAreaView style={[styles.safeArea, { backgroundColor: theme.background }]} edges={['bottom']}>
      <ThemedView style={styles.container}>
        {/* Search bar */}
        <ThemedView style={styles.searchBar}>
          <TextInput
            style={[styles.input, { color: theme.text, borderColor: theme.textSecondary + '60', backgroundColor: theme.background }]}
            placeholder="搜索文件…"
            placeholderTextColor={theme.textSecondary}
            value={query}
            onChangeText={setQuery}
            onSubmitEditing={handleSearch}
            returnKeyType="search"
            autoCapitalize="none"
            autoCorrect={false}
          />
          <Pressable
            style={({ pressed }) => [
              styles.searchBtn,
              { backgroundColor: pressed ? '#197935' : '#1f883d', opacity: loading ? 0.6 : 1 },
            ]}
            onPress={handleSearch}
            disabled={loading}
          >
            {loading ? (
              <ActivityIndicator size="small" color="#fff" />
            ) : (
              <ThemedText style={styles.searchBtnText}>搜索</ThemedText>
            )}
          </Pressable>
        </ThemedView>

        {/* Filter chips */}
        <ThemedView style={styles.filterRow}>
          {filters.map((f) => (
            <Pressable
              key={f.key}
              style={({ pressed }) => [
                styles.filterChip,
                {
                  backgroundColor: type === f.key ? '#0969da' : theme.backgroundElement,
                  opacity: pressed ? 0.8 : 1,
                },
              ]}
              onPress={() => setType(f.key)}
            >
              <ThemedText
                style={[
                  styles.filterChipText,
                  { color: type === f.key ? '#fff' : theme.text },
                ]}
              >
                {f.label}
              </ThemedText>
            </Pressable>
          ))}
        </ThemedView>

        {/* Error */}
        {error ? (
          <ThemedView type="backgroundElement" style={styles.errorBox}>
            <ThemedText style={styles.errorText}>{error}</ThemedText>
          </ThemedView>
        ) : null}

        {/* Results */}
        {loading ? (
          <ThemedView style={styles.center}>
            <ActivityIndicator size="large" color={theme.text} />
          </ThemedView>
        ) : searched && results.length === 0 ? (
          <ThemedView style={styles.center}>
            <ThemedText style={styles.emptyText}>未找到匹配的文件</ThemedText>
          </ThemedView>
        ) : (
          <FlatList
            data={results}
            keyExtractor={(item) => item.id}
            renderItem={renderItem}
            contentContainerStyle={styles.list}
          />
        )}
      </ThemedView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
  },
  container: {
    flex: 1,
    maxWidth: MaxContentWidth,
    width: '100%',
    alignSelf: 'center',
  },
  searchBar: {
    flexDirection: 'row',
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two,
    gap: Spacing.two,
  },
  input: {
    flex: 1,
    borderWidth: 1,
    borderRadius: 6,
    paddingHorizontal: 12,
    paddingVertical: 8,
    fontSize: 16,
  },
  searchBtn: {
    paddingHorizontal: 16,
    borderRadius: 6,
    justifyContent: 'center',
    alignItems: 'center',
    minWidth: 60,
  },
  searchBtnText: {
    color: '#fff',
    fontSize: 15,
    fontWeight: 600,
  },
  filterRow: {
    flexDirection: 'row',
    paddingHorizontal: Spacing.three,
    paddingBottom: Spacing.two,
    gap: Spacing.two,
  },
  filterChip: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
  },
  filterChipText: {
    fontSize: 13,
    fontWeight: 500,
  },
  errorBox: {
    marginHorizontal: Spacing.three,
    padding: Spacing.two,
    borderRadius: 6,
    borderWidth: 1,
    borderColor: '#cf222e',
    marginBottom: Spacing.two,
  },
  errorText: {
    color: '#cf222e',
    fontSize: 14,
  },
  center: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  emptyText: {
    fontSize: 16,
    opacity: 0.5,
  },
  list: {
    paddingBottom: Spacing.two,
  },
  item: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.three,
    paddingVertical: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  itemIcon: {
    fontSize: 20,
    marginRight: Spacing.two,
  },
  itemInfo: {
    flex: 1,
  },
  itemName: {
    fontSize: 16,
    fontWeight: 500,
  },
  itemPath: {
    fontSize: 12,
    marginTop: 1,
    opacity: 0.5,
  },
  itemSize: {
    fontSize: 13,
    opacity: 0.5,
    marginLeft: Spacing.two,
  },
});
