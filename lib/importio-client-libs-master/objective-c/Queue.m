//
//  Queue.m
//  iOS Client Library
//
//  Created by dev on 23/09/2013.
//  Copyright (c) 2013 importio. All rights reserved.
//

#import "Queue.h"

@implementation Queue

- (id) init {
    underlyingArray = [[NSMutableArray alloc] init];
    conditionLock = [[NSCondition alloc] init];
    return self;
}

- (void) enqueue: (id)item {
    
    [conditionLock lock];
    [underlyingArray addObject:item];
    [conditionLock signal];
    [conditionLock unlock];
    
}

- (id) get {
    
    [conditionLock lock];
    
    id item = nil;
    
    while ([underlyingArray count] == 0) {
        [conditionLock wait];
    }
    
    item = [underlyingArray objectAtIndex:0];
    [underlyingArray removeObjectAtIndex:0];
    
    [conditionLock unlock];
    return item;
}

- (id) peek {
    id item = nil;
    if ([underlyingArray count] != 0) {
        item = [underlyingArray objectAtIndex:0];
    }
    return item;
}

@end
