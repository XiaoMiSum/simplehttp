# GitHub Actions Secrets 配置

本文档说明如何为 simplehttp 项目配置 GitHub Actions 所需的 Secrets。

## 必需的 Secrets

| Secret 名称 | 说明 | 获取方式 |
|------------|------|---------|
| `MAVEN_USERNAME` | Maven Central 账号用户名 | [Sonatype JIRA](https://issues.sonatype.org/) 注册 |
| `MAVEN_PASSWORD` | Maven Central 账号密码 | 注册时设置或重置密码 |
| `GPG_PRIVATE_KEY` | GPG 私钥（ASCII 格式） | 使用 `gpg --export-secret-keys --armor` 导出 |
| `GPG_PASSPHRASE` | GPG 私钥的密码短语 | 生成 GPG 密钥时设置 |

## 配置步骤

### 方式一：使用 GitHub Web 界面（推荐新手）

1. 打开 GitHub 仓库页面
2. 点击 **Settings** → **Secrets and variables** → **Actions**
3. 点击 **New repository secret**
4. 依次添加以下 4 个 Secrets：

#### 1. MAVEN_USERNAME
- **Name**: `MAVEN_USERNAME`
- **Secret**: 你的 Maven Central 用户名

#### 2. MAVEN_PASSWORD
- **Name**: `MAVEN_PASSWORD`  
- **Secret**: 你的 Maven Central 密码

#### 3. GPG_PRIVATE_KEY
- **Name**: `GPG_PRIVATE_KEY`
- **Secret**: 完整的 GPG 私钥内容

**如何获取 GPG 私钥：**

```bash
# 1. 列出你的 GPG 密钥
gpg --list-secret-keys --keyid-format LONG

# 输出示例：
# sec   rsa4096/XXXXXXXXXXXXXXXX 2024-01-01 [SC]
#       XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
# uid         [ 绝对 ] Your Name <your.email@example.com>

# 2. 导出私钥（替换 XXXXXXXXXXXXXXXX 为你的密钥 ID）
gpg --export-secret-keys --armor XXXXXXXXXXXXXXXX

# 3. 输入 GPG 密码短语后，会输出完整的私钥
# 复制从 -----BEGIN PGP PRIVATE KEY BLOCK----- 到 -----END PGP PRIVATE KEY BLOCK----- 的全部内容
```

#### 4. GPG_PASSPHRASE
- **Name**: `GPG_PASSPHRASE`
- **Secret**: 生成 GPG 密钥时设置的密码短语

### 方式二：使用 GitHub CLI（推荐高级用户）

如果你已安装 [GitHub CLI](https://cli.github.com/)，可以使用提供的脚本快速配置：

```bash
# 运行配置脚本
./setup-github-secrets.sh
```

或者手动使用 `gh` 命令：

```bash
# 设置 Maven 凭据
gh secret set MAVEN_USERNAME -b "your-maven-username"
gh secret set MAVEN_PASSWORD -b "your-maven-password"

# 设置 GPG 密钥
gh secret set GPG_PASSPHRASE -b "your-gpg-passphrase"

# 从文件导入 GPG 私钥
gh secret set GPG_PRIVATE_KEY < private.key
```

### 方式三：使用 GitHub API

```bash
# 设置 Secrets（需要 Personal Access Token）
curl -X PUT \
  -H "Authorization: token YOUR_GITHUB_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  https://api.github.com/repos/YOUR_USERNAME/simplehttp/actions/secrets/MAVEN_USERNAME \
  -d '{"encrypted_value":"your-encrypted-value","key_id":"public-key-id"}'
```

## 验证配置

### 检查 Secrets 是否已设置

```bash
# 使用 GitHub CLI 列出所有 Secrets（不显示值）
gh secret list
```

或在 GitHub 页面上查看 **Settings** → **Secrets and variables** → **Actions**

### 测试工作流

1. 推送任意代码变更到 master/main 分支
2. 查看 **Actions** 页面，确认 CI 工作流正常运行
3. 手动触发发布工作流测试（使用测试版本号）

## 常见问题

### Q1: GPG 私钥格式错误

**问题**：工作流报告 GPG 私钥格式错误

**解决**：
- 确保导出的私钥包含完整的 `-----BEGIN PGP PRIVATE KEY BLOCK-----` 和 `-----END PGP PRIVATE KEY BLOCK-----`
- 使用 `--armor` 参数导出 ASCII 格式的私钥
- 检查私钥内容是否完整，没有多余的空格或换行

### Q2: 认证失败 (401 Unauthorized)

**问题**：发布时报告 401 错误

**解决**：
- 检查 `MAVEN_USERNAME` 和 `MAVEN_PASSWORD` 是否正确
- 确认 Maven Central 账号处于激活状态
- 尝试在本地使用相同的凭据登录

### Q3: GPG 签名失败

**问题**：`gpg: signing failed: Inappropriate ioctl for device`

**解决**：
- 工作流已配置 `--pinentry-mode loopback`，通常不会出现此问题
- 如果仍有问题，检查 `GPG_PASSPHRASE` 是否正确

### Q4: 找不到 GPG 公钥

**问题**：`No public key` 或验证失败

**解决**：
```bash
# 确保公钥已上传到密钥服务器
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID

# 验证公钥是否已上传
gpg --keyserver keyserver.ubuntu.com --search-keys YOUR_EMAIL
```

## 安全建议

1. **定期轮换密钥**
   - 每 3-6 个月更新 Maven Central 密码
   - 建议每年重新生成 GPG 密钥对

2. **保护 Secrets**
   - 不要将 Secrets 提交到代码仓库
   - 不要在 Issue、PR 或评论中泄露 Secrets
   - 限制有权限访问 Secrets 的人员

3. **启用双重认证**
   - 为 GitHub 账号启用 2FA
   - 为 Maven Central 账号启用 2FA（如果支持）

4. **审计日志**
   - 定期检查 GitHub Actions 运行日志
   - 监控 Maven Central 的发布记录

## 生成 GPG 密钥（如果还没有）

```bash
# 1. 生成新的 GPG 密钥对
gpg --full-generate-key

# 选择：
# - 密钥类型: RSA and RSA (default)
# - 密钥大小: 4096
# - 有效期: 0 (永不过期)
# - 用户名: 与 GitHub 关联的邮箱
# - 密码短语: 设置一个强密码

# 2. 列出密钥
gpg --list-secret-keys --keyid-format LONG

# 3. 导出私钥（用于配置 GitHub Secret）
gpg --export-secret-keys --armor YOUR_KEY_ID > private.key

# 4. 上传公钥到密钥服务器
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

## 相关文档

- [GitHub Actions Secrets 文档](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Maven Central 发布指南](https://central.sonatype.org/publish/publish-guide/)
- [GPG 密钥管理](https://www.gnupg.org/documentation/manuals/gnupg/)
- [本项目发布指南](../PUBLISH_GUIDE.md)
