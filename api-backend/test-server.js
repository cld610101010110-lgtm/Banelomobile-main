#!/usr/bin/env node
/**
 * Quick Server Health Check
 * Run this to verify API server is working correctly
 */

const http = require('http');
const os = require('os');

console.log('\n🔍 BANELO API SERVER HEALTH CHECK\n');
console.log('='.repeat(50));

// Get computer's IP addresses
function getIPAddresses() {
    const interfaces = os.networkInterfaces();
    const addresses = [];

    for (const name of Object.keys(interfaces)) {
        for (const iface of interfaces[name]) {
            // Skip internal and non-IPv4 addresses
            if (iface.family === 'IPv4' && !iface.internal) {
                addresses.push({ name, address: iface.address });
            }
        }
    }

    return addresses;
}

// Test if a URL is reachable
function testEndpoint(url, description) {
    return new Promise((resolve) => {
        const startTime = Date.now();

        http.get(url, (res) => {
            const responseTime = Date.now() - startTime;
            let data = '';

            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                try {
                    const json = JSON.parse(data);
                    console.log(`✅ ${description}`);
                    console.log(`   URL: ${url}`);
                    console.log(`   Status: ${res.statusCode}`);
                    console.log(`   Response time: ${responseTime}ms`);
                    if (json.success !== undefined) {
                        console.log(`   Success: ${json.success}`);
                    }
                    if (Array.isArray(json.data)) {
                        console.log(`   Data count: ${json.data.length} items`);
                    }
                    console.log('');
                    resolve(true);
                } catch (e) {
                    console.log(`⚠️  ${description} - Invalid JSON response`);
                    console.log(`   URL: ${url}`);
                    console.log('');
                    resolve(false);
                }
            });
        }).on('error', (err) => {
            console.log(`❌ ${description}`);
            console.log(`   URL: ${url}`);
            console.log(`   Error: ${err.message}`);
            console.log('');
            resolve(false);
        }).setTimeout(5000, function() {
            console.log(`❌ ${description} - Timeout`);
            console.log(`   URL: ${url}`);
            console.log('');
            this.destroy();
            resolve(false);
        });
    });
}

// Main test sequence
async function runTests() {
    console.log('\n📍 YOUR COMPUTER\'S IP ADDRESSES:\n');

    const ipAddresses = getIPAddresses();
    if (ipAddresses.length === 0) {
        console.log('❌ No network interfaces found!');
        console.log('   Check if you\'re connected to Wi-Fi\n');
    } else {
        ipAddresses.forEach(({ name, address }) => {
            console.log(`   ${name}: ${address}`);
        });
        console.log('');
    }

    console.log('='.repeat(50));
    console.log('\n🧪 TESTING API ENDPOINTS:\n');

    // Test localhost
    await testEndpoint('http://localhost:3000/api/products', 'API on localhost:3000');

    // Test each IP address
    for (const { address } of ipAddresses) {
        await testEndpoint(`http://${address}:3000/api/products`, `API on ${address}:3000`);
    }

    // Test the configured IP from BaneloApiService.kt
    const configuredIP = '192.168.254.176';
    if (!ipAddresses.some(ip => ip.address === configuredIP)) {
        console.log(`⚠️  WARNING: BaneloApiService.kt is configured to use ${configuredIP}`);
        console.log(`   but this IP is not found on your computer!`);
        console.log(`   You may need to update BaneloApiService.kt line 202\n`);

        await testEndpoint(`http://${configuredIP}:3000/api/products`, `API on ${configuredIP}:3000 (configured)`);
    }

    console.log('='.repeat(50));
    console.log('\n📋 SUMMARY:\n');

    if (ipAddresses.length > 0) {
        console.log('To connect from your mobile device:');
        console.log('');
        console.log('1. Ensure your phone is on the SAME Wi-Fi network');
        console.log('2. Update BaneloApiService.kt line 202 to use one of these IPs:');
        ipAddresses.forEach(({ address }) => {
            console.log(`   private const val BASE_URL = "http://${address}:3000/"`);
        });
        console.log('');
        console.log('3. Test in phone\'s browser first:');
        ipAddresses.forEach(({ address }) => {
            console.log(`   http://${address}:3000/api/products`);
        });
    }

    console.log('\n' + '='.repeat(50) + '\n');
}

// Check if server is likely running by testing localhost first
console.log('Checking if server is running...\n');

const testReq = http.get('http://localhost:3000/api/products', (res) => {
    console.log('✅ Server is running!\n');
    runTests();
}).on('error', (err) => {
    console.log('❌ Server is NOT running!');
    console.log(`   Error: ${err.message}`);
    console.log('');
    console.log('Please start the server first:');
    console.log('   cd api-backend');
    console.log('   node server.js');
    console.log('');
    console.log('Then run this test again.');
    console.log('');
});

testReq.setTimeout(3000, function() {
    console.log('❌ Server is NOT responding on localhost:3000');
    console.log('');
    console.log('Please start the server:');
    console.log('   cd api-backend');
    console.log('   node server.js');
    console.log('');
    this.destroy();
});
