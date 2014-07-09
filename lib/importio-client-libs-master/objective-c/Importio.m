//
//  importio.m
//  iOS Client Library
//
//  Created by dev on 23/09/2013.
//  Copyright (c) 2013 importio. All rights reserved.
//

#import "Importio.h"
#import "Queue.h"

@implementation Importio

// message ID
int msgId;

bool _connected;

NSString* clientId;

NSString* _userId;
NSString* _apiKey;

NSString* rootUrl = @"https://query.import.io";

Queue* messageQueue;
NSString* messagingChannel = @"/messaging";

NSMutableDictionary* queries;

- (id) init {
    messageQueue = [[Queue alloc] init];
    queries = [[NSMutableDictionary alloc] init];
    
    _connected = FALSE;
    
    return self;
}

- (id) initWithUserIdAndApikey:(NSString *)userId withApikey:(NSString *)apiKey
{
    _userId = [userId copy];
    _apiKey = [apiKey copy];
    
    messageQueue = [[Queue alloc] init];
    queries = [[NSMutableDictionary alloc] init];
    
    _connected = FALSE;
    
    return self;
}

- (NSString*)urlEncode: (NSString*) unescaped
{
    return (NSString *)CFBridgingRelease(CFURLCreateStringByAddingPercentEscapes(NULL, (__bridge CFStringRef)unescaped, NULL, CFSTR("!*'();:@&=+$,/?%#[]"), kCFStringEncodingUTF8));
}

- (void)login:(NSString *) username withPassword:(NSString *) password withHost:(NSString *)host
{
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
    
    NSString *post = [NSString  stringWithFormat:@"username=%@&password=%@",[self urlEncode:username],[self urlEncode:password]];
    
    NSData *postData = [post dataUsingEncoding:NSUTF8StringEncoding allowLossyConversion:NO];
    
    NSString *postLength = [NSString stringWithFormat:@"%d", [postData length]];
    
    [request setURL:[NSURL URLWithString:[host stringByAppendingString:@"/auth/login"] ]];
    [request setHTTPMethod:@"POST"];
    [request setValue:@"application/x-www-form-urlencoded" forHTTPHeaderField:@"Content-Type"];
    [request setValue:postLength forHTTPHeaderField:@"Content-Length"];
    [request setHTTPBody:postData];
    
    NSHTTPURLResponse *response;
    NSData *returnData = [NSURLConnection sendSynchronousRequest:request returningResponse:&response error:nil];
    NSLog(@"%@",[NSString stringWithFormat:@"status:%d", [response statusCode]]);
    NSLog(@"%@",[[NSString alloc] initWithData:returnData encoding:NSUTF8StringEncoding]);
    
    
}

- (NSArray*) request: (NSString*) channel withPath: (NSString*) path withData: (NSMutableDictionary*) data withThrows: (BOOL) throw
{
    
    // Add in the column values
    data[@"channel"] = channel;
    data[@"connectionType"] = @"long-polling";
    data[@"id"] =  [NSString stringWithFormat:@"%d", msgId];
    
    msgId++;
    
    if (clientId != NULL)
    {
        data[@"clientId"] = clientId;
    }
    
    NSString* url = [NSString stringWithFormat:@"%@/query/comet/%@", rootUrl, path];
    
    if (_apiKey != NULL)
    {
        url = [NSString stringWithFormat:@"%@?&_user=%@&_apikey=%@", url,[self urlEncode:_userId],[self urlEncode:_apiKey]];
    }
    
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
    
    [request setHTTPShouldHandleCookies:YES];
    
    NSHTTPCookieStorage* cookieJar = [NSHTTPCookieStorage sharedHTTPCookieStorage];
    
    NSDictionary* headers = [NSHTTPCookie requestHeaderFieldsWithCookies:[cookieJar cookies]];
    [request setAllHTTPHeaderFields:headers];
    
    NSData* postData;
    
    if (data != nil) {
        postData = [NSJSONSerialization dataWithJSONObject:[NSArray arrayWithObjects: data, nil]
                                                   options:kNilOptions
                                                     error:nil];
    }
    
    NSString* postLength;
    
    if (postData != nil)
    {
        postLength = [NSString stringWithFormat:@"%d", [postData length]];
        [request setValue:postLength forHTTPHeaderField:@"Content-Length"];
        [request setHTTPBody:postData];
    }
    
    [request setURL:[NSURL URLWithString:url ]];
    [request setHTTPMethod:@"POST"];
    [request setValue:@"application/json;charset=UTF-8" forHTTPHeaderField:@"Content-Type"];
    [request setValue:@"gzip" forHTTPHeaderField:@"Accept-Encoding"];
    
    NSHTTPURLResponse *response;
    NSError* error = nil;
    NSData *returnData = [NSURLConnection sendSynchronousRequest:request returningResponse:&response error:&error];
    
    if (error)
    {
        NSLog(@"Error occurred %@",error);
    }
    
    NSArray* jsonMessages = [NSJSONSerialization JSONObjectWithData:returnData
                                                            options:kNilOptions
                                                              error:nil];
    
    for(NSDictionary* message in jsonMessages)
    {
        if ([message objectForKey:@"successful"] != nil && [message[@"successful"]isEqual:@"false"])
        {
            NSString* errorMsg = [[NSString alloc] initWithData:returnData encoding:NSUTF8StringEncoding];
            NSLog(@"%@", [NSString stringWithFormat:@"Unsuccessful request: %@", errorMsg]);
            if(throw)
            {
                @throw [NSException exceptionWithName:@"Import io Exception" reason:@"request was not successfully sent" userInfo:nil];
            }
            
        }
        if ([message objectForKey:@"data"]) {
            [messageQueue enqueue:message[@"data"]];
        }
        
    }
    return jsonMessages;
}

- (void) handshake
{
    NSMutableDictionary* handshakeData = [@{@"version":@"1.0",@"minimumVersion":@"0.9",@"supportedConnectionTypes":@[@"long-polling"],@"advice":@{@"timeout":@60000,@"interval":@0}} mutableCopy];
    NSArray* handshake = [self request:@"/meta/handshake" withPath:@"handshake" withData:handshakeData withThrows:TRUE];
    
    // Store a reference to the client id
    clientId = handshake[0][@"clientId"];
}

- (void) connect
{
    if(_connected) {
        return;
    }
    [self handshake];
    
    NSMutableDictionary* subscriptionData = [@{@"subscription":messagingChannel} mutableCopy];
    [self request:@"/meta/subscribe" withPath:@"" withData:subscriptionData withThrows:FALSE];
    
    _connected = TRUE;
    
    NSThread* pollThread = [[NSThread alloc] initWithTarget:self selector:@selector(poll) object:nil];
    [pollThread start];
    
    NSThread* messageThread = [[NSThread alloc] initWithTarget:self selector:@selector(pollQueue) object:nil];
    [messageThread start];
    
}

- (void) disconnect
{
    [self request:@"/meta/disconnect" withPath:@"" withData:[[NSMutableDictionary alloc] init] withThrows:TRUE];
    _connected = FALSE;
}

- (void) pollQueue
{
    while (_connected) {
        [self processMessage:[messageQueue get]];
    }
}

- (void) poll
{
    while (_connected) {
        [self request:@"/meta/connect" withPath:@"connect" withData:[[NSMutableDictionary alloc] init] withThrows:FALSE];
    }
}

- (void) processMessage: (NSDictionary*) data
{
    @try {
        NSString* reqId = data[@"requestId"];
        Query* query = queries[reqId];
        
        [query onMessage:data];
        
        if ([query finished])
        {
            [queries removeObjectForKey:reqId];
        }
    }
    @catch (NSException* e) {
        NSLog(@"Name:%@", e.name);
        NSLog(@"Reason:%@", e.reason);
    }
}

- (NSString*) uuidString
{
    CFUUIDRef uuid = CFUUIDCreate(kCFAllocatorDefault);
    NSString* uuidStr = (__bridge_transfer NSString*)CFUUIDCreateString(kCFAllocatorDefault, uuid);
    CFRelease(uuid);
    return uuidStr;
    
}

- (void) query: (NSDictionary*) query withCallback:(void(^)(Query*,NSDictionary*)) callback
{
    NSString* requestId = [self uuidString];
    queries[requestId] = [[Query alloc] initWithInputsAndCallback:query withRequestId:requestId withCallback:callback];
    
    NSMutableDictionary* queryWithRequestId = [query mutableCopy];
    queryWithRequestId[@"requestId"] = requestId;
    NSMutableDictionary* queryData = [@{@"data":queryWithRequestId} mutableCopy];
    [self request:@"/service/query" withPath:@"" withData:queryData withThrows:FALSE];
}

@end

@implementation Query

- (id) initWithInputsAndCallback: (NSDictionary*) input withRequestId: (NSString*) requestId withCallback: (void (^)(Query*, NSDictionary*)) callback
{
    // Have to copy these here so it doesnt get garbage collected
    _requestId = [requestId copy];
    _input = [input copy];
    _onMessage = callback;
    
    return self;
}

- (void) onMessage: (NSDictionary*) message
{
    NSString* type = message[@"type"];
    
    if([type isEqual:@"INIT"] || [type isEqual:@"START"]) {
        _jobsStarted++;
    } else if([type isEqual:@"SPAWN"]) {
        _jobsSpawned++;
    } else if([type isEqual:@"STOP"]) {
        _jobsCompleted++;
    }
    
    _finished = _jobsStarted == _jobsCompleted && _jobsSpawned+1 == _jobsStarted && _jobsStarted >0;
    
    // If there is an error or the user is not authorised correctly then allow finished to returne true
    if([type isEqual:@"ERROR"] || [type isEqual:@"UNAUTH"] || [type isEqual:@"CANCEL"]) {
        _finished = true;
    }
    
    _onMessage(self,message);
}

@end
