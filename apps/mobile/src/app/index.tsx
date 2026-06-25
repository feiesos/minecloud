import { useState, useEffect, useCallback } from 'react';
import { StyleSheet, FlatList, Pressable, RefreshControl, ActivityIndicator, Alert } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ThemedView } from '@/components/themed-view';
import { ThemedText } from '@/components/themed-text';
import { Spacing, MaxContentWidth } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { listFiles, type FileItem } from '@/api/files';
import DirIcon from '@/components/icons/DirIcon';
import FileIcon from '@/components/icons/FileIcon';

interface Breadcrumb {
  id: string;
  name: string;
}

export default function FileBrowser() {
  const theme = useTheme();
  const [items, setItems] = useState<FileItem[]>([]);
  const [breadcrumb, setBreadcrumb] = useState<Breadcrumb[]>([{ id: '0', name: 'root' }]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');

  const currentParentId = breadcrumb[breadcrumb.length - 1].id;

  const loadFiles = useCallback(async (parentId: string) => {
    try {
      const data = await listFiles(parentId);
      setItems(data);
      setError('');
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载失败');
    }
  }, []);

  useEffect(() => {
    setLoading(true);
    loadFiles(currentParentId).finally(() => setLoading(false));
  }, [currentParentId, loadFiles]);

  async function onRefresh() {
    setRefreshing(true);
    await loadFiles(currentParentId);
    setRefreshing(false);
  }

  function navigateToDir(id: string, name: string) {
    setBreadcrumb((prev) => [...prev, { id, name }]);
  }

  function navigateBreadcrumb(index: number) {
    setBreadcrumb((prev) => prev.slice(0, index + 1));
  }

  function formatDate(dateStr: string): string {
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    } catch {
      return dateStr;
    }
  }

  function formatSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return `${(bytes / Math.pow(1024, i)).toFixed(i > 0 ? 1 : 0)} ${units[i]}`;
  }

  function renderItem({ item }: { item: FileItem }) {
    return (
      <Pressable
        style={({ pressed }) => [
          styles.item,
          { backgroundColor: pressed ? theme.backgroundElement : theme.background },
          { borderBottomColor: theme.textSecondary + '30' },
        ]}
        onPress={() => {
          if (item.isDir) {
            navigateToDir(item.id, item.name);
          } else {
            Alert.alert(item.name, `大小: ${formatSize(item.size)}\n创建时间: ${formatDate(item.createTime)}`);
          }
        }}
      >
        <ThemedView style={styles.itemIcon}>
          {item.isDir ? <DirIcon size={22} /> : <FileIcon name={item.name} size={22} />}
        </ThemedView>
        <ThemedView style={styles.itemInfo}>
          <ThemedText style={styles.itemName} numberOfLines={1}>{item.name}</ThemedText>
          <ThemedText style={styles.itemMeta}>
            {item.isDir ? '文件夹' : formatSize(item.size)} · {formatDate(item.createTime)}
          </ThemedText>
        </ThemedView>
      </Pressable>
    );
  }

  return (
    <SafeAreaView style={[styles.safeArea, { backgroundColor: theme.background }]} edges={['bottom']}>
      <ThemedView style={styles.container}>
        {/* Breadcrumb */}
        <ThemedView style={styles.breadcrumbBar}>
          {breadcrumb.map((seg, i) => (
            <ThemedView key={i} style={styles.bcSegment}>
              {i > 0 && <ThemedText style={styles.bcSep}>/</ThemedText>}
              {i === breadcrumb.length - 1 ? (
                <ThemedText style={styles.bcCurrent} numberOfLines={1}>{seg.name}</ThemedText>
              ) : (
                <Pressable onPress={() => navigateBreadcrumb(i)}>
                  <ThemedText style={styles.bcLink}>{seg.name}</ThemedText>
                </Pressable>
              )}
            </ThemedView>
          ))}
        </ThemedView>

        {/* Error */}
        {error ? (
          <ThemedView type="backgroundElement" style={styles.errorBox}>
            <ThemedText style={styles.errorText}>{error}</ThemedText>
          </ThemedView>
        ) : null}

        {/* File list */}
        {loading && items.length === 0 ? (
          <ThemedView style={styles.center}>
            <ActivityIndicator size="large" color={theme.text} />
          </ThemedView>
        ) : items.length === 0 ? (
          <ThemedView style={styles.center}>
            <ThemedText style={styles.emptyText}>此目录为空</ThemedText>
          </ThemedView>
        ) : (
          <FlatList
            data={items}
            keyExtractor={(item) => item.id}
            renderItem={renderItem}
            refreshControl={
              <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={theme.text} />
            }
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
  breadcrumbBar: {
    flexDirection: 'row',
    alignItems: 'center',
    flexWrap: 'wrap',
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two,
    gap: 2,
  },
  bcSegment: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  bcSep: {
    marginHorizontal: 4,
    fontSize: 14,
  },
  bcLink: {
    fontSize: 14,
    color: '#0969da',
  },
  bcCurrent: {
    fontSize: 14,
    fontWeight: 600,
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
    width: 32,
    alignItems: 'center',
    justifyContent: 'center',
  },
  itemInfo: {
    flex: 1,
  },
  itemName: {
    fontSize: 16,
    fontWeight: 500,
  },
  itemMeta: {
    fontSize: 12,
    marginTop: 2,
    opacity: 0.6,
  },
});
