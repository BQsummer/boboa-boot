'use client';

import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { ProtectedRoute } from '@/components/auth/protected-route';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { ApiError } from '@/lib/api/client';
import {
  deleteSystemFile,
  getSystemFileList,
  SystemFileItem,
  updateSystemFile,
  uploadSystemFile,
} from '@/lib/api/system-files';

interface UploadFormState {
  file: File | null;
  category: string;
}

interface EditFormState {
  fileName: string;
  category: string;
}

const EMPTY_UPLOAD_FORM: UploadFormState = {
  file: null,
  category: 'system',
};

const EMPTY_EDIT_FORM: EditFormState = {
  fileName: '',
  category: '',
};

const DEFAULT_PAGE_SIZE = 10;

function SystemFilesPageContent() {
  const [listLoading, setListLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [deletingKey, setDeletingKey] = useState<string | null>(null);
  const [notice, setNotice] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const [searchInput, setSearchInput] = useState('');
  const [categoryInput, setCategoryInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const [category, setCategory] = useState('');

  const [page, setPage] = useState(1);
  const [pageSize] = useState(DEFAULT_PAGE_SIZE);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [files, setFiles] = useState<SystemFileItem[]>([]);

  const [uploadForm, setUploadForm] = useState<UploadFormState>(EMPTY_UPLOAD_FORM);
  const [editOpen, setEditOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<SystemFileItem | null>(null);
  const [editForm, setEditForm] = useState<EditFormState>(EMPTY_EDIT_FORM);

  const loadFiles = useCallback(async (targetPage: number, targetKeyword: string, targetCategory: string) => {
    try {
      setListLoading(true);
      setNotice(null);
      const result = await getSystemFileList({
        page: targetPage,
        pageSize,
        keyword: targetKeyword,
        category: targetCategory,
      });
      setFiles(Array.isArray(result.list) ? result.list : []);
      setPage(Number(result.page || targetPage));
      setTotal(Number(result.total || 0));
      setTotalPages(Math.max(Number(result.totalPages || 1), 1));
    } catch (error) {
      const message =
        error instanceof ApiError
          ? `Load files failed (HTTP ${error.status})`
          : error instanceof Error
            ? error.message
            : 'Load files failed';
      setNotice({ type: 'error', text: message });
      setFiles([]);
      setTotal(0);
      setTotalPages(1);
    } finally {
      setListLoading(false);
    }
  }, [pageSize]);

  useEffect(() => {
    loadFiles(1, '', '');
  }, [loadFiles]);

  const handleSearch = async (event: FormEvent) => {
    event.preventDefault();
    const nextKeyword = searchInput.trim();
    const nextCategory = categoryInput.trim();
    setKeyword(nextKeyword);
    setCategory(nextCategory);
    await loadFiles(1, nextKeyword, nextCategory);
  };

  const handleReset = async () => {
    setSearchInput('');
    setCategoryInput('');
    setKeyword('');
    setCategory('');
    await loadFiles(1, '', '');
  };

  const handleUpload = async (event: FormEvent) => {
    event.preventDefault();
    if (!uploadForm.file) {
      setNotice({ type: 'error', text: 'Please choose one file.' });
      return;
    }
    try {
      setUploading(true);
      setNotice(null);
      await uploadSystemFile({
        file: uploadForm.file,
        category: uploadForm.category,
      });
      setNotice({ type: 'success', text: 'File uploaded.' });
      setUploadForm(EMPTY_UPLOAD_FORM);
      await loadFiles(1, keyword, category);
    } catch (error) {
      const message =
        error instanceof ApiError
          ? `Upload failed (HTTP ${error.status})`
          : error instanceof Error
            ? error.message
            : 'Upload failed';
      setNotice({ type: 'error', text: message });
    } finally {
      setUploading(false);
    }
  };

  const openEdit = (item: SystemFileItem) => {
    setEditingItem(item);
    setEditForm({
      fileName: item.fileName || '',
      category: item.category || '',
    });
    setEditOpen(true);
  };

  const handleUpdate = async (event: FormEvent) => {
    event.preventDefault();
    if (!editingItem?.fileKey) {
      return;
    }
    try {
      setUpdating(true);
      setNotice(null);
      await updateSystemFile({
        key: editingItem.fileKey,
        fileName: editForm.fileName.trim(),
        category: editForm.category.trim(),
      });
      setNotice({ type: 'success', text: 'File renamed/moved.' });
      setEditOpen(false);
      setEditingItem(null);
      await loadFiles(page, keyword, category);
    } catch (error) {
      const message =
        error instanceof ApiError
          ? `Update failed (HTTP ${error.status})`
          : 'Update failed';
      setNotice({ type: 'error', text: message });
    } finally {
      setUpdating(false);
    }
  };

  const handleDelete = async (key: string) => {
    if (!window.confirm(`Delete file: ${key}?`)) {
      return;
    }
    try {
      setDeletingKey(key);
      setNotice(null);
      await deleteSystemFile(key);
      setNotice({ type: 'success', text: 'File deleted.' });
      await loadFiles(page, keyword, category);
    } catch (error) {
      const message =
        error instanceof ApiError
          ? `Delete failed (HTTP ${error.status})`
          : 'Delete failed';
      setNotice({ type: 'error', text: message });
    } finally {
      setDeletingKey(null);
    }
  };

  const canPrev = page > 1 && !listLoading;
  const canNext = page < totalPages && !listLoading;
  const showRange = useMemo(() => {
    if (total === 0 || files.length === 0) {
      return '0 - 0';
    }
    const start = (page - 1) * pageSize + 1;
    const end = start + files.length - 1;
    return `${start} - ${end}`;
  }, [files.length, page, pageSize, total]);

  return (
    <div className="space-y-6 p-6">
      <div>
        <h1 className="text-2xl font-bold">文件管理</h1>
        <p className="text-sm text-gray-500">直接读取存储后端文件列表，不依赖数据库。</p>
      </div>

      {notice ? (
        <Card className={notice.type === 'success' ? 'border-green-300' : 'border-red-300'}>
          <CardContent className="p-4">
            <p className={notice.type === 'success' ? 'text-green-700' : 'text-red-700'}>{notice.text}</p>
          </CardContent>
        </Card>
      ) : null}

      <Card>
        <CardHeader>
          <CardTitle>上传文件</CardTitle>
          <CardDescription>文件会按分类写入当前配置的存储后端。</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="grid gap-4 md:grid-cols-4" onSubmit={handleUpload}>
            <div className="md:col-span-2">
              <label className="mb-1 block text-sm font-medium">File</label>
              <Input
                type="file"
                onChange={(event) => setUploadForm((prev) => ({ ...prev, file: event.target.files?.[0] || null }))}
                disabled={uploading}
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium">Category</label>
              <Input
                value={uploadForm.category}
                onChange={(event) => setUploadForm((prev) => ({ ...prev, category: event.target.value }))}
                disabled={uploading}
              />
            </div>
            <div className="md:col-span-4">
              <Button type="submit" disabled={uploading}>
                {uploading ? 'Uploading...' : 'Upload'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>文件列表</CardTitle>
          <CardDescription>按关键字和分类筛选，支持重命名/移动与删除。</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <form className="flex flex-col gap-3 md:flex-row" onSubmit={handleSearch}>
            <Input
              placeholder="Keyword in key"
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
            />
            <Input
              placeholder="Category"
              value={categoryInput}
              onChange={(event) => setCategoryInput(event.target.value)}
            />
            <div className="flex gap-2">
              <Button type="submit" disabled={listLoading}>Search</Button>
              <Button type="button" variant="outline" onClick={handleReset} disabled={listLoading}>Reset</Button>
            </div>
          </form>

          {listLoading ? (
            <p className="text-sm">Loading...</p>
          ) : files.length === 0 ? (
            <p className="text-sm text-gray-500">No files found.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-[1080px] w-full text-sm">
                <thead>
                  <tr className="border-b text-left">
                    <th className="py-2 pr-2">Name</th>
                    <th className="py-2 pr-2">Category</th>
                    <th className="py-2 pr-2">Storage</th>
                    <th className="py-2 pr-2">Size</th>
                    <th className="py-2 pr-2">Key</th>
                    <th className="py-2 pr-2">URL</th>
                    <th className="py-2 pr-2">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {files.map((item) => (
                    <tr key={item.fileKey} className="border-b align-top">
                      <td className="py-2 pr-2">{item.fileName || '-'}</td>
                      <td className="py-2 pr-2">{item.category || '-'}</td>
                      <td className="py-2 pr-2">{item.storageType || '-'}</td>
                      <td className="py-2 pr-2">{item.sizeBytes ?? '-'}</td>
                      <td className="py-2 pr-2 max-w-[320px] break-all">{item.fileKey || '-'}</td>
                      <td className="py-2 pr-2">
                        {item.accessUrl ? (
                          <a className="text-blue-600 underline" href={item.accessUrl} target="_blank" rel="noreferrer">
                            Open
                          </a>
                        ) : (
                          '-'
                        )}
                      </td>
                      <td className="py-2 pr-2">
                        <div className="flex gap-2">
                          <Button size="sm" variant="outline" onClick={() => openEdit(item)}>Edit</Button>
                          <Button
                            size="sm"
                            variant="outline"
                            disabled={deletingKey === item.fileKey}
                            onClick={() => handleDelete(item.fileKey)}
                          >
                            {deletingKey === item.fileKey ? 'Deleting...' : 'Delete'}
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <div className="flex items-center justify-between">
            <p className="text-sm text-gray-600">
              Showing {showRange} of {total} items
            </p>
            <div className="flex items-center gap-2">
              <Button variant="outline" size="sm" disabled={!canPrev} onClick={() => loadFiles(page - 1, keyword, category)}>
                Prev
              </Button>
              <span className="text-sm">Page {page} / {totalPages}</span>
              <Button variant="outline" size="sm" disabled={!canNext} onClick={() => loadFiles(page + 1, keyword, category)}>
                Next
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {editOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <Card className="w-full max-w-2xl">
            <CardHeader className="flex flex-row items-start justify-between">
              <div>
                <CardTitle>重命名/移动</CardTitle>
                <CardDescription className="max-w-xl break-all">{editingItem?.fileKey}</CardDescription>
              </div>
              <Button type="button" variant="outline" onClick={() => setEditOpen(false)} disabled={updating}>
                Close
              </Button>
            </CardHeader>
            <CardContent>
              <form className="grid gap-4 md:grid-cols-2" onSubmit={handleUpdate}>
                <div>
                  <label className="mb-1 block text-sm font-medium">File Name</label>
                  <Input
                    value={editForm.fileName}
                    onChange={(event) => setEditForm((prev) => ({ ...prev, fileName: event.target.value }))}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium">Category</label>
                  <Input
                    value={editForm.category}
                    onChange={(event) => setEditForm((prev) => ({ ...prev, category: event.target.value }))}
                  />
                </div>
                <div className="flex items-end justify-end gap-2 md:col-span-2">
                  <Button type="button" variant="outline" onClick={() => setEditOpen(false)} disabled={updating}>
                    Cancel
                  </Button>
                  <Button type="submit" disabled={updating}>
                    {updating ? 'Saving...' : 'Save'}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>
      ) : null}
    </div>
  );
}

export default function SystemFilesPage() {
  return (
    <ProtectedRoute>
      <SystemFilesPageContent />
    </ProtectedRoute>
  );
}

