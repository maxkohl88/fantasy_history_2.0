//
//  importio.h
//  iOS Client Library
//
//  Created by dev on 23/09/2013.
//  Copyright (c) 2013 importio. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface Query : NSObject {
    void (^_onMessage)(Query*, NSDictionary*);
    NSString* _requestId;
    NSDictionary* _input;
}

@property bool finished;

@property int jobsStarted;
@property int jobsCompleted;
@property int jobsSpawned;


- (void) onMessage: (NSDictionary*) message;

- (id) initWithInputsAndCallback: (NSDictionary*) input withRequestId: (NSString*) requestId withCallback: (void (^)(Query*,NSDictionary*)) callback;


@end

@interface Importio : NSObject


- (id) initWithUserIdAndApikey: (NSString*) userId withApikey: (NSString*) apiKey;

- (void) login: (NSString*) username withPassword: (NSString*) password withHost: (NSString*) host;

- (void) query: (NSDictionary*) query withCallback:(void (^)(Query*,NSDictionary*)) callback;

- (void) connect;

- (void) disconnect;

@end



