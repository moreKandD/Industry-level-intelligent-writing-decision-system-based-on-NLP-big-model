document.addEventListener('DOMContentLoaded', () => {
    // ================== 应用状态管理 ==================
    const appState = {
        isGenerating: false,
        progressInterval: null,
        steps: [
            { id: 1, name: '主题分析', desc: '解析核心关键词' },
            { id: 2, name: '数据收集', desc: '获取行业最新数据' },
            { id: 3, name: '素材整合', desc: '分析用户提供内容' },
            { id: 4, name: '大纲生成', desc: '创建文档结构框架' },
            { id: 5, name: '内容优化', desc: '深度润色与增强' },
            { id: 6, name: '格式转换', desc: '生成最终文档' }
        ],
        currentProgress: 0,
        fileName: null
    };

    // ================== DOM元素缓存（安全获取） ==================
    const DOM = {
        get generateBtn() {
            return document.querySelector('.generate-btn') || this.createFallbackElement('button', '生成按钮');
        },
        get downloadBtn() {
            return document.getElementById('download-btn') || this.createFallbackElement('button', '下载按钮');
        },
        get themeInput() {
            return document.getElementById('theme') || this.createFallbackElement('textarea', '主题输入框');
        },
        get materialInput() {
            return document.getElementById('material') || this.createFallbackElement('textarea', '素材输入框');
        },
        get progressBar() {
            return document.getElementById('progress-bar') || this.createProgressBar();
        },
        get stepIndicator() {
            return document.getElementById('step-indicator') || this.createStepIndicator();
        },
        get industrySelect() {
            return document.getElementById('industry');
        },
        get lengthSelect() {
            return document.getElementById('length');
        },
        outputArea: document.getElementById('output') || document.body,

        // 备用元素创建方法
        createFallbackElement(tag, name) {
            console.warn(`自动创建备用${name}`);
            const el = document.createElement(tag);
            el.textContent = name;
            document.body.appendChild(el);
            return el;
        },

        // 创建进度条容器
        createProgressBar() {
            console.warn('自动创建进度条容器');
            const container = document.createElement('div');
            container.id = 'progress-bar';
            container.innerHTML = '<div style="width:0%;height:15px;background:#3498db;transition:width 0.3s"></div>';
            document.body.appendChild(container);
            return container;
        },

        // 创建步骤指示器容器
        createStepIndicator() {
            console.warn('自动创建步骤指示器');
            const container = document.createElement('div');
            container.id = 'step-indicator';
            container.className = 'step-container';
            document.body.appendChild(container);
            return container;
        }
    };

    // ================== 初始化应用 ==================
    function initApp() {
        restoreSavedValues();
        setupValueSaving();
        if (!validateEssentialDOMElements()) {
            showMessage('系统初始化失败，请刷新页面', 'error');
            return;
        }

        try {
            renderSteps();
            setupEventListeners();
            DOM.downloadBtn.disabled = true;
            showMessage('系统准备就绪', 'success');
        } catch (error) {
            console.error('初始化失败:', error);
            showMessage('系统初始化异常，请联系管理员', 'error');
        }
    }

    // 初始化时恢复保存的值
    function restoreSavedValues() {
        DOM.industrySelect.value = localStorage.getItem('industry') || '市场营销';
        DOM.lengthSelect.value = localStorage.getItem('length') || '标准模式';
        document.getElementById('creativity').value = localStorage.getItem('creativity') || 5;
    }

    function setupValueSaving() {
        DOM.industrySelect.addEventListener('change', () => {
            localStorage.setItem('industry', DOM.industrySelect.value);
        });
        DOM.lengthSelect.addEventListener('change', () => {
            localStorage.setItem('length', DOM.lengthSelect.value);
        });
        document.getElementById('creativity').addEventListener('input', function() {
            localStorage.setItem('creativity', this.value);
        });
    }

    // ================== 核心功能函数 ==================
    function validateEssentialDOMElements() {
        const requiredElements = [
            DOM.generateBtn,
            DOM.themeInput,
            DOM.materialInput,
            DOM.progressBar,
            DOM.stepIndicator
        ];

        return requiredElements.every(el => {
            if (!el || !el.isConnected) {
                console.error('关键元素缺失:', el);
                return false;
            }
            return true;
        });
    }

    function renderSteps() {
        try {
            DOM.stepIndicator.innerHTML = appState.steps.map(step => `
                <div class="step" data-step="${step.id}">
                    <div class="step-icon">${step.id}</div>
                    <div class="step-name">${step.name}</div>
                    <div class="step-desc">${step.desc}</div>
                </div>
            `).join('');
        } catch (error) {
            console.error('步骤渲染失败:', error);
            DOM.stepIndicator.innerHTML = '<div class="error">步骤初始化失败</div>';
        }
    }

    function setupEventListeners() {
        // 生成按钮
        DOM.generateBtn.addEventListener('click', async () => {
            try {
                await handleGeneration();
            } catch (error) {
                handleError(error);
            }
        });

        // 下载按钮
        DOM.downloadBtn.addEventListener('click', async () => {
            try {
                await handleDownload();
            } catch (error) {
                handleError(error);
            }
        });

        // ================== 输入验证函数 ==================
    function validateInput(inputElement, minLength) {
        try {
            const value = inputElement.value.trim();
            const isValid = value.length >= minLength;
            
            // 视觉反馈
            inputElement.style.borderColor = isValid ? '#2ecc71' : '#e74c3c';
            inputElement.parentNode.querySelector('.hint')?.remove(); // 移除旧提示
            
            // 动态提示
            if (!isValid) {
                const hint = document.createElement('small');
                hint.className = 'hint';
                hint.style.cssText = `
                    display: block;
                    color: #e74c3c;
                    font-size: 0.8rem;
                    margin-top: 5px;
                `;
                hint.textContent = `至少需要${minLength}个字符`;
                inputElement.parentNode.appendChild(hint);
            }
            
            return isValid;
        } catch (error) {
            console.error('输入验证异常:', error);
            return false;
        }
    }

        // 输入验证
        DOM.themeInput.addEventListener('input', () => validateInput(DOM.themeInput, 1));
        DOM.materialInput.addEventListener('input', () => validateInput(DOM.materialInput, 20));
    }

    // ================== 业务逻辑函数 ==================
    async function handleGeneration() {
        if (appState.isGenerating) return showMessage('文档生成中，请稍候...', 'warning');
        if (!validateForm()) return;
    
        try {
            initializeGenerationState();
            startProgressSimulation();
            
            const response = await fetch('http://localhost:8080/api/generate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    theme: DOM.themeInput.value.trim(),
                    material: DOM.materialInput.value.trim(),
                    length: document.getElementById('length').value,
                    creativity: parseInt(document.getElementById('creativity').value),
                    industry: document.getElementById('industry').value
                })
            });
    
            const data = await handleApiResponse(response);
            handleSuccess(data);
        } catch (error) {
            handleError(error);
        } finally {
            finalizeGeneration();
        }
    }
    
    function startProgressSimulation() {
        let progress = 0;
        const simulation = setInterval(() => {
            if (progress >= 90 || !appState.isGenerating) {
                clearInterval(simulation);
                return;
            }
            progress += Math.random() * 15;
            appState.progress = Math.min(progress, 90);
            updateProgressDisplay();
        }, 800);
    }
    
    async function handleApiResponse(response) {
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || `请求失败: ${response.status}`);
        }
        return response.json();
    }
    
    function handleSuccess(data) {
        appState.progress = 100;
        updateProgressDisplay();
        appState.fileName = data.fileName;
        DOM.downloadBtn.disabled = false;
        DOM.outputArea.textContent = "文档生成成功！点击下载按钮获取文件。";
        showMessage('文档已准备就绪', 'success');
    }

    async function processStep(index) {
        updateStepState(index, 'processing');
        
        // 分阶段进度更新
        const totalSteps = appState.steps.length;
        const baseProgress = (index / totalSteps) * 100;
        
        for (let p = 0; p <= 100; p += 20) {
            appState.progress = Math.min(baseProgress + (p / 100) * (100 / totalSteps), 100);
            updateProgressDisplay();
            await delay(300);
        }
        
        updateStepState(index, 'done');
        showMessage(`已完成：${appState.steps[index].name}`, 'success');
    }

    // ================== 工具类函数 ==================
    function updateStepState(index, status) {
        const stepElements = DOM.stepIndicator.querySelectorAll('.step');
        if (!stepElements[index]) return;

        const stepElement = stepElements[index];
        switch(status) {
            case 'processing':
                stepElement.classList.add('active');
                stepElement.classList.remove('done');
                stepElement.querySelector('.step-icon').innerHTML = '⏳';
                break;
            case 'done':
                stepElement.classList.add('done');
                stepElement.classList.remove('active');
                stepElement.querySelector('.step-icon').innerHTML = '✓';
                appState.steps[index].done = true;
                break;
        }
    }

    function updateProgressDisplay() {
        const progressBar = DOM.progressBar.querySelector('div');
        if (progressBar) {
            progressBar.style.width = `${appState.progress}%`;
        }
        
        // 百分比显示
        let percentDisplay = DOM.progressBar.querySelector('.percent');
        if (!percentDisplay) {
            percentDisplay = document.createElement('div');
            percentDisplay.className = 'percent';
            percentDisplay.style.cssText = `
                position: absolute;
                right: 20px;
                top: 50%;
                transform: translateY(-50%);
                font-weight: bold;
                color: #2c3e50;
            `;
            DOM.progressBar.appendChild(percentDisplay);
        }
        percentDisplay.textContent = `${Math.floor(appState.progress)}%`;
    }

    // ============== 工具函数 ==============
    function initApp() {
        // ============== 第一阶段：环境安全检查 ==============
        checkRuntimeEnvironment();
        
        // ============== 第二阶段：输入持久化配置 ==============
        setupInputPersistence();
        
        // ============== 第三阶段：基础初始化 ==============
        try {
            // 初始化步骤指示器
            renderSteps();
            
            // 绑定事件监听器
            setupEventListeners();
            
            // 禁用下载按钮初始状态
            DOM.downloadBtn.disabled = true;
            
            // 显示就绪状态
            showMessage('系统准备就绪', 'success', 2000);
            
            // 恢复上次的滚动位置
            restoreScrollPosition();
        } catch (error) {
            console.error('初始化失败:', error);
            showMessage('系统初始化异常，部分功能可能受限', 'error');
        }
    }
    
    function checkRuntimeEnvironment() {
        const isLocalhost = window.location.hostname === 'localhost';
        const isHttps = window.location.protocol === 'https:';
        
        if (!isLocalhost && !isHttps) {
            showMessage(
                '安全警告：建议使用HTTPS安全连接以保证数据传输安全', 
                'warning',
                5000
            );
        }
    }

    function setupInputPersistence() {
        // 需要持久化的字段配置
        const persistFields = [
            { id: 'theme', defaultValue: '' },
            { id: 'material', defaultValue: '' },
            { id: 'industry', defaultValue: '市场营销' },
            { id: 'length', defaultValue: '标准模式' },
            { id: 'creativity', defaultValue: '5' }
        ];

        // 恢复存储的值
        persistFields.forEach(({id, defaultValue}) => {
            const el = document.getElementById(id);
            if (el) {
                el.value = localStorage.getItem(id) || defaultValue;
            }
        });

        // 设置输入监听（防抖处理）
        persistFields.forEach(({id}) => {
            const el = document.getElementById(id);
            if (el) {
                el.addEventListener('input', debounce(() => {
                    localStorage.setItem(id, el.value);
                }, 300));
            }
        });
    }

    function restoreScrollPosition() {
        const savedPosition = sessionStorage.getItem('scrollPosition');
        if (savedPosition) {
            window.scrollTo(0, parseInt(savedPosition));
            sessionStorage.removeItem('scrollPosition');
        }
    }

    function debounce(func, wait) {
        let timeout;
        return (...args) => {
            clearTimeout(timeout);
            timeout = setTimeout(() => func.apply(this, args), wait);
        };
    }


    

    // ================== 文档生成逻辑 ==================
    function validateForm() {
        const isValid = {
            theme: DOM.themeInput.value.trim().length > 0,
            material: DOM.materialInput.value.trim().length >= 20
        };

        // 更新输入框状态
        DOM.themeInput.style.borderColor = isValid.theme ? '' : '#e74c3c';
        DOM.materialInput.style.borderColor = isValid.material ? '' : '#e74c3c';

        // 显示错误信息
        if (!isValid.theme) showMessage('请输入文档主题！', 'error');
        if (!isValid.material) showMessage('素材内容需至少20字', 'warning');

        return isValid.theme && isValid.material;
    }

    function initializeGenerationState() {
        appState.isGenerating = true;
        appState.currentStep = 0;
        appState.progress = 0;
        DOM.generateBtn.disabled = true;
        DOM.downloadBtn.disabled = true;
        
        // 重置步骤状态
        DOM.stepIndicator.querySelectorAll('.step').forEach(step => {
            step.classList.remove('active', 'done');
            const stepNumber = step.dataset.step;
            step.querySelector('.step-icon').textContent = stepNumber;
        });
    }

    function finalizeGeneration() {
        appState.isGenerating = false;
        DOM.generateBtn.disabled = false;
        
        // 重置进度条
        const innerBar = DOM.progressBar.querySelector('div');
        if (innerBar) {
            innerBar.style.width = '0%';
        }
    }

    function handleResponse(data) {
        if (data.fileName) {
            appState.fileName = data.fileName;
            DOM.downloadBtn.disabled = false;
            showMessage('文档生成成功！', 'success');
        } else {
            throw new Error(data.error || '文档生成失败');
        }
    }

    // ================== 下载处理逻辑 ==================
    async function handleDownload() {
        if (!appState.fileName) {
            return showMessage('请先生成文档', 'warning');
        }
    
        try {
            const message = showMessage('文档下载准备中...', 'loading');
            const response = await fetch(`http://localhost:8080/api/download/${appState.fileName}`);
            
            if (!response.ok) {
                throw new Error(`下载失败: ${response.status}`);
            }
            
            const blob = await response.blob();
            triggerDownload(blob);
            message.remove();
            showMessage('下载完成！', 'success');
        } catch (error) {
            showMessage(`下载失败: ${error.message}`, 'error');
        }
    }
    
    function triggerDownload(blob) {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `行业分析报告_${new Date().toLocaleDateString().replace(/\//g, '-')}.md`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    }

    async function fetchDocument() {
        const response = await fetch(`http://localhost:8080/api/download/${appState.fileName}`);
        if (!response.ok) throw new Error(`下载失败: ${response.status}`);
        return await response.blob();
    }

    function triggerDownload(blob) {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `行业分析报告_${new Date().toLocaleDateString().replace(/\//g, '-')}.md`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    }

    // ================== 消息提示系统 ==================
    function showMessage(text, type = 'info') {
        const colors = {
            error: '#e74c3c',
            success: '#2ecc71',
            warning: '#f1c40f',
            loading: '#3498db',
            info: '#2c3e50'
        };

        const iconMap = {
            error: '❌',
            success: '✅',
            warning: '⚠️',
            loading: '⏳'
        };

        const message = document.createElement('div');
        message.className = `message ${type}`;
        message.innerHTML = `
            <span style="color: ${colors[type]}">
                ${iconMap[type] || ''}
                ${text}
            </span>
        `;

        // 添加动画
        message.style.opacity = '0';
        message.style.transform = 'translateY(20px)';
        DOM.outputArea.appendChild(message);
        
        requestAnimationFrame(() => {
            message.style.transition = 'all 0.3s ease';
            message.style.opacity = '1';
            message.style.transform = 'translateY(0)';
        });

        // 自动移除
        if (type !== 'loading') {
            setTimeout(() => {
                message.style.opacity = '0';
                setTimeout(() => message.remove(), 300);
            }, 5000);
        }

        return message;
    }

    function handleError(error) {
        console.error('操作失败:', error);
        showMessage(`操作失败: ${error.message}`, 'error');
    }

    // ================== 通用工具函数 ==================
    const delay = ms => new Promise(resolve => setTimeout(resolve, ms));

    // ================== 启动应用 ==================
    initApp();
});


