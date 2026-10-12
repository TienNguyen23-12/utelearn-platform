const fs = require('fs');
const path = require('path');
const baseDir = 'd:/Izhary/HK1_2627/LTWeb/utelearn-platform/utelearn/src/main/resources/templates';

function getFiles(dir, fileList = []) {
    const files = fs.readdirSync(dir);
    for (const file of files) {
        const filePath = path.join(dir, file);
        if (fs.statSync(filePath).isDirectory()) {
            getFiles(filePath, fileList);
        } else if (filePath.endsWith('.html')) {
            fileList.push(filePath);
        }
    }
    return fileList;
}

const htmlFiles = getFiles(baseDir);

const newShowToast = `    function showToast(message, type) {
        let iconHtml = '';
        if (type === 'success') {
            iconHtml = \\\`<div style="background-color: #00b050; border-radius: 50%; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; flex-shrink: 0;"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg></div>\\\`;
        } else if (type === 'warning') {
            iconHtml = \\\`<div style="background-color: #f5a623; border-radius: 50%; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; flex-shrink: 0;"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg></div>\\\`;
        } else {
            iconHtml = \\\`<div style="background-color: #dc3545; border-radius: 50%; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; flex-shrink: 0;"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg></div>\\\`;
        }

        let toast = Toastify({
            text: \`<div style="display: flex; align-items: center; gap: 10px;">
                    \${iconHtml}
                    <span style="color: #111; font-weight: 500; font-size: 14.5px; font-family: 'Plus Jakarta Sans', sans-serif;">\${message}</span>
                   </div>\`,
            escapeMarkup: false,
            duration: 3000,
            close: false,
            gravity: "top",
            position: "right",
            style: {
                background: "white",
                borderRadius: "30px",
                boxShadow: "0 6px 16px rgba(0,0,0,0.12)",
                padding: "10px 20px 10px 12px",
                border: "1px solid #f0f0f0",
                cursor: "pointer"
            },
            onClick: function() {
                toast.hideToast();
            }
        });
        toast.showToast();
    }`;

for (const file of htmlFiles) {
    let content = fs.readFileSync(file, 'utf8');
    let changed = false;

    const oldShowToastRegex = /function\s+showToast\s*\(\s*message\s*,\s*type\s*\)\s*\{[\s\S]*?\}\)\.showToast\(\);\s*\}/g;
    if (oldShowToastRegex.test(content)) {
        content = content.replace(oldShowToastRegex, newShowToast);
        changed = true;
    }

    if (content.includes("showToast(Dang nh?p")) {
        content = content.replace(/showToast\(Dang nh\?p thnh cng!, success\);/g, "showToast('Đăng nhập thành công!', 'success');");
        if (!content.includes("function showToast")) {
            content = content.replace(/<\/script>\s*<\/body>/, newShowToast + "\n</script>\n</body>");
        }
        changed = true;
    }

    if (file.endsWith('dashboard\\index.html') || file.endsWith('dashboard/index.html')) {
        const inlineToastifyRegex = /Toastify\(\{[\s\S]*?\}\)\.showToast\(\);/g;
        if (inlineToastifyRegex.test(content)) {
            content = content.replace(inlineToastifyRegex, "showToast('Đăng nhập thành công!', 'success');");
            if (!content.includes("function showToast")) {
                content = content.replace(/<\/script>\s*<\/body>/, "\n" + newShowToast + "\n</script>\n</body>");
            }
            changed = true;
        }
    }

    if (changed) {
        fs.writeFileSync(file, content, 'utf8');
        console.log("Updated: " + file);
    }
}
